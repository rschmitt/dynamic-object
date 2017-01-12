package com.github.rschmitt.dynamicobject.internal;

import com.github.rschmitt.collider.ClojureList;
import com.github.rschmitt.collider.ClojureMap;
import com.github.rschmitt.collider.ClojureSet;
import com.github.rschmitt.collider.Collider;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class Conversions {
    interface Converter<T> {
        T getter(Object obj);
        Object builder(T t);
    }

    private static final ConcurrentMap<Class<?>, Converter<?>> converters = new ConcurrentHashMap<>();

    static <T> void register(Class<T> type, Converter<T> converter) { converters.put(type, converter); }
    static <T> void deregister(Class<T> type) { converters.remove(type); }

    static class OptionalConverter implements Converter<Optional> {
        public Optional getter(Object obj) { return Optional.ofNullable(obj); }
        public Object builder(Optional o) { return o.orElse(null); }
    }

    static class MapConverter implements Converter<ClojureMap> {
        public ClojureMap getter(Object obj) { return Collider.intoClojureMap((Map) obj); }
        public Object builder(ClojureMap map) { return map.unwrap(); }
    }

    static class ListConverter implements Converter<ClojureList> {
        public ClojureList getter(Object obj) { return Collider.intoClojureList((List) obj); }
        public Object builder(ClojureList map) { return map.unwrap(); }
    }

    static class SetConverter implements Converter<ClojureSet> {
        public ClojureSet getter(Object obj) { return Collider.intoClojureSet((Set) obj); }
        public Object builder(ClojureSet map) { return map.unwrap(); }
    }

    static class InstantConverter implements Converter<Instant> {
        public Instant getter(Object obj) { return ((Date) obj).toInstant(); }
        public Object builder(Instant instant) { return Date.from(instant); }
    }

    static {
        register(Optional.class, new OptionalConverter());
        register(Instant.class, new InstantConverter());

        register(ClojureMap.class, new MapConverter());
        register(ClojureList.class, new ListConverter());
        register(ClojureSet.class, new SetConverter());
    }

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
        for (Class type : converters.keySet()) {
            if (type.isAssignableFrom(obj.getClass())) {
                Converter converter = converters.get(type);
                return converter.builder(obj);
            }
        }
        if (true) return obj;
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
            obj = convertCollectionToJavaTypes((Collection<?>) obj, ClojureStuff.EmptyVector, genericReturnType);
            if (rawReturnType.equals(ClojureList.class))
                return Collider.intoClojureList((List) obj);
        } else if (obj instanceof Set) {
            obj = convertCollectionToJavaTypes((Collection<?>) obj, ClojureStuff.EmptySet, genericReturnType);
            if (rawReturnType.equals(ClojureSet.class))
                return Collider.intoClojureSet((Set) obj);
        } else if (obj instanceof Map) {
            obj = convertMapToJavaTypes((Map<?, ?>) obj, genericReturnType);
            if (rawReturnType.equals(ClojureMap.class))
                return Collider.intoClojureMap((Map) obj);
        }
        return obj;
    }

    private static Object convertCollectionToJavaTypes(Collection<?> coll, Object empty, Type genericReturnType) {
        Object ret = ClojureStuff.Transient.invoke(empty);
        for (Object o : coll)
            ret = ClojureStuff.ConjBang.invoke(ret, convertCollectionElementToJavaTypes(o, genericReturnType));
        return ClojureStuff.Persistent.invoke(ret);
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
        Object ret = ClojureStuff.Transient.invoke(ClojureStuff.EmptyMap);
        for (Map.Entry<?, ?> entry : unwrappedMap.entrySet())
            ret = ClojureStuff.AssocBang.invoke(ret, clojureToJava(entry.getKey(), keyType), clojureToJava(entry.getValue(), valType));
        return ClojureStuff.Persistent.invoke(ret);
    }
}
