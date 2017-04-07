package com.github.rschmitt.dynamicobject.internal;

import com.github.rschmitt.collider.ClojureList;
import com.github.rschmitt.collider.ClojureMap;
import com.github.rschmitt.collider.ClojureSet;
import com.github.rschmitt.collider.Collider;
import com.github.rschmitt.collider.TransientMap;
import com.github.rschmitt.dynamicobject.DynamicObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;

class Conversions {
    /*
     * Convert a Java object (e.g. passed in to a builder method) into the Clojure-style representation used internally.
     * This is done according to the following rules:
     *  * Boxed and unboxed numerics, as well as BigInteger, will be losslessly converted to Long, Double, or BigInt.
     *  * Values wrapped in an Optional will be unwrapped and stored as either null or the underlying value.
     *  * Supported collection types (List, Set, Map) will have their elements converted according to these rules. This
     *    also applies to nested collections. For instance, a List<List<Integer>> will effectively be converted to a
     *    List<List<Long>>.
     */
    static Object javaToClojure(Object obj) {
        Object val = Numerics.maybeUpconvert(obj);
        if (val instanceof DynamicObject)
            return obj;
        else if (val instanceof Instant)
            return java.util.Date.from((Instant) val);
        else if (val instanceof List)
            return convertCollectionToClojureTypes((Collection<?>) val, ClojureStuff.EmptyVector);
        else if (val instanceof Set)
            return convertCollectionToClojureTypes((Collection<?>) val, ClojureStuff.EmptySet);
        else if (val instanceof Map)
            return convertMapToClojureTypes((Map<?, ?>) val);
        else if (val instanceof Optional) {
            Optional<?> opt = (Optional<?>) val;
            if (opt.isPresent())
                return javaToClojure(opt.get());
            else
                return null;
        } else
            return val;
    }

    private static Object convertCollectionToClojureTypes(Collection<?> val, Object empty) {
        Object ret = ClojureStuff.Transient.invoke(empty);
        for (Object o : val)
            ret = ClojureStuff.ConjBang.invoke(ret, javaToClojure(o));
        return ClojureStuff.Persistent.invoke(ret);
    }

    private static Object convertMapToClojureTypes(Map<?, ?> map) {
        Object ret = ClojureStuff.Transient.invoke(ClojureStuff.EmptyMap);
        for (Map.Entry<?, ?> entry : map.entrySet())
            ret = ClojureStuff.AssocBang.invoke(ret, javaToClojure(entry.getKey()), javaToClojure(entry.getValue()));
        return ClojureStuff.Persistent.invoke(ret);
    }

    /*
     * Convert a Clojure object (i.e. a value somewhere in a DynamicObject's map) into the expected Java representation.
     * This representation is determined by the generic return type of the method. The conversion is performed as
     * follows:
     *  * If the return type is a numeric type, the Clojure numeric will be downconverted to the expected type (e.g.
     *    Long -> Integer).
     *  * If the return type is a nested DynamicObject, we wrap the Clojure value as the expected DynamicObject type.
     *  * If the return type is an Optional, we convert the value and then wrap it by calling Optional#ofNullable.
     *  * If the return type is a collection type, there are a few possibilities:
     *    * If it is a raw type, no action is taken.
     *    * If it is a wildcard type (e.g. List<?>), an UnsupportedOperationException is thrown.
     *    * If the type variable is a Class, the elements of the collection are enumerated over to convert numerics and
     *      wraps DynamicObjects.
     *    * If the type variable is another collection type, the algorithm recurses.
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

        if (obj instanceof List) {
            obj = ((Collection<?>)obj).stream().map(
                    elem -> convertCollectionElementToJavaTypes(elem, genericReturnType)
            ).collect(Collider.toClojureList());
        } else if (obj instanceof Set) {
            obj = ((Collection<?>)obj).stream().map(
                    elem -> convertCollectionElementToJavaTypes(elem, genericReturnType)
            ).collect(Collider.toClojureSet());
        } else if (obj instanceof Map) {
            obj = convertMapToJavaTypes((Map<?, ?>) obj, genericReturnType);
            if (rawReturnType.equals(ClojureMap.class))
                return Collider.intoClojureMap((Map) obj);
        }
        return obj;
    }

    private static Object convertCollectionElementToJavaTypes(Object element, Type genericCollectionType) {
        if (genericCollectionType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericCollectionType;
            List<Type> typeArgs = Arrays.asList(parameterizedType.getActualTypeArguments());
            assert typeArgs.size() == 1;
            return clojureToJava(element, typeArgs.get(0));
        } else
            return clojureToJava(element, Object.class);
    }

    private static Object convertMapToJavaTypes(Map<?, ?> unwrappedMap, Type genericReturnType) {
        Type keyType, valType;
        if (genericReturnType instanceof ParameterizedType) {
            Type[] actualTypeArguments = ((ParameterizedType) genericReturnType).getActualTypeArguments();
            assert actualTypeArguments.length == 2;
            keyType = actualTypeArguments[0];
            valType = actualTypeArguments[1];
        } else {
            keyType = valType = Object.class;
        }

        TransientMap<Object, Object> transientMap = Collider.transientMap();

        for (Map.Entry<?, ?> entry : unwrappedMap.entrySet())
            transientMap.put(clojureToJava(entry.getKey(), keyType), clojureToJava(entry.getValue(), valType));

        return transientMap.toPersistent();
    }
}
