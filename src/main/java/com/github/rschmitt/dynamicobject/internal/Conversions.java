package com.github.rschmitt.dynamicobject.internal;

import com.github.rschmitt.dynamicobject.DynamicObject;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

class Conversions {
    /*
     * Convert a Java object (e.g. passed in to a builder method) into the Clojure-style representation used internally.
     * This is done according to the following rules:
     *  * Boxed and unboxed numerics, as well as BigInteger, will be losslessly converted to Long, Double, or BigInt.
     *  * Values wrapped in an Optional will be unwrapped and stored as either null or the underlying value.
     */
    static Object javaToClojure(Object obj) {
        Object val = Numerics.maybeUpconvert(obj);
        if (val instanceof DynamicObject)
            return obj;
        else if (val instanceof Instant)
            return java.util.Date.from((Instant) val);
        else if (val instanceof Optional) {
            Optional<?> opt = (Optional<?>) val;
            if (opt.isPresent())
                return javaToClojure(opt.get());
            else
                return null;
        } else
            return val;
    }

    /*
     * Convert a Clojure object (i.e. a value somewhere in a DynamicObject's map) into the expected Java representation.
     * This representation is determined by the generic return type of the method. The conversion is performed as
     * follows:
     *  * If the return type is a numeric type, the Clojure numeric will be downconverted to the expected type (e.g.
     *    Long -> Integer).
     *  * If the return type is a nested DynamicObject, we wrap the Clojure value as the expected DynamicObject type.
     *  * If the return type is an Optional, we convert the value and then wrap it by calling Optional#ofNullable.
     */
    @SuppressWarnings("unchecked")
    static Object clojureToJava(Object obj, Type genericReturnType) {
        Class<?> rawReturnType = Reflection.getRawType(genericReturnType);
        if (rawReturnType.equals(Optional.class)) {
            Type nestedType = Reflection.getTypeArgument(genericReturnType, 0);
            return Optional.ofNullable(clojureToJava(obj, nestedType));
        }

        if (obj == null) return null;
        if (genericReturnType instanceof Class) {
            Class<?> returnType = (Class<?>) genericReturnType;
            if (Numerics.isNumeric(returnType))
                return Numerics.maybeDownconvert(returnType, obj);
            if (Instant.class.equals(returnType))
                return ((Date) obj).toInstant();
            if (DynamicObject.class.isAssignableFrom(returnType))
                return DynamicObject.wrap((Map) obj, (Class<? extends DynamicObject>) returnType);
        }

        return obj;
    }
}
