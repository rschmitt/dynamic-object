package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.AFn;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.rschmitt.dynamicobject.ClojureStuff.*;

class DynamicObjectInvocationHandler<T extends DynamicObject<T>> implements InvocationHandler {
    private static final Object DEFAULT = new Object();
    private static final Object NULL = new Object();

    private final Object map;
    private final Class<T> type;
    private final Constructor<MethodHandles.Lookup> lookupConstructor;
    private final ConcurrentHashMap valueCache = new ConcurrentHashMap();

    DynamicObjectInvocationHandler(Object map, Class<T> type, Constructor<MethodHandles.Lookup> lookupConstructor) {
        this.map = map;
        this.type = type;
        this.lookupConstructor = lookupConstructor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if (isBuilderMethod(method)) {
            if (isMetadataBuilder(method))
                return assocMeta(methodName, args[0]);
            Object val = maybeUpconvert(args[0]);
            val = Erasure.unwrapCollectionElements(val, List.class, EMPTY_VECTOR);
            val = Erasure.unwrapCollectionElements(val, Set.class, EMPTY_SET);
            val = Erasure.unwrapMapElements(val);
            String key = getBuilderKey(method);
            return assoc(key, val);
        }

        if (method.isDefault())
            return invokeDefaultMethod(proxy, method, args);

        switch (methodName) {
            case "getMap":
                return map;
            case "getType":
                return type;
            case "toString":
                return map.toString();
            case "hashCode":
                return map.hashCode();
            case "prettyPrint":
                PPRINT.invoke(map);
                return null;
            case "toFormattedString":
                Writer w = new StringWriter();
                PPRINT.invoke(map, w);
                return w.toString();
            case "merge":
                return merge((DynamicObject<T>) args[0]);
            case "equals":
                Object other = args[0];
                if (other instanceof DynamicObject)
                    return map.equals(((DynamicObject) other).getMap());
                else
                    return method.invoke(map, args);
            default:
                if (isMetadataGetter(method))
                    return getMetadataFor(methodName);
                return getAndCacheValueFor(method);
        }
    }

    private T merge(DynamicObject<T> other) {
        AFn ignoreNulls = new AFn() {
            public Object invoke(Object arg1, Object arg2) {
                return (arg2 == null) ? arg1 : arg2;
            }
        };
        Object mergedMap = MERGE_WITH.invoke(ignoreNulls, map, other.getMap());
        return DynamicObject.wrap(mergedMap, type);
    }

    @SuppressWarnings("unchecked")
    private Object getAndCacheValueFor(Method method) {
        Object cachedValue = valueCache.getOrDefault(method, DEFAULT);
        if (cachedValue != DEFAULT) return cachedValue;
        if (cachedValue == NULL) return null;
        Object value = getValueFor(method);
        if (value == null)
            valueCache.putIfAbsent(method, NULL);
        else
            valueCache.putIfAbsent(method, value);
        return value;
    }

    private static String getBuilderKey(Method method) {
        for (Annotation[] annotations : method.getParameterAnnotations())
            for (Annotation annotation : annotations)
                if (annotation.annotationType().equals(Key.class)) {
                    String key = ((Key) annotation).value();
                    if (key.charAt(0) == ':')
                        key = key.substring(1);
                    return key;
                }
        return method.getName();
    }

    private static Object maybeUpconvert(Object val) {
        if (val instanceof Float) val = Double.parseDouble(String.valueOf(val));
        else if (val instanceof Short) val = Long.valueOf((short) val);
        else if (val instanceof Byte) val = Long.valueOf((byte) val);
        else if (val instanceof Integer) val = Long.valueOf((int) val);
        return val;
    }

    private T assoc(String key, Object value) {
        Object keyword = cachedRead(":" + key);
        if (value instanceof DynamicObject)
            value = ((DynamicObject) value).getMap();
        return DynamicObject.wrap(ASSOC.invoke(map, keyword, value), type);
    }

    private Object assocMeta(String key, Object value) {
        Object meta = META.invoke(map);
        if (meta == null)
            meta = EMPTY_MAP;
        meta = ASSOC.invoke(meta, key, value);
        return DynamicObject.wrap(WITH_META.invoke(map, meta), type);
    }

    private boolean isBuilderMethod(Method method) {
        return method.getReturnType().equals(type) && method.getParameterCount() == 1;
    }

    private static boolean isMetadataBuilder(Method method) {
        if (method.getParameterCount() != 1)
            return false;
        for (Annotation[] annotations : method.getParameterAnnotations())
            for (Annotation annotation : annotations)
                if (annotation.annotationType().equals(Meta.class))
                    return true;
        return false;
    }

    private Object getMetadataFor(String key) {
        Object meta = META.invoke(map);
        return GET.invoke(meta, key);
    }

    private static boolean isMetadataGetter(Method method) {
        if (method.getParameterCount() != 0)
            return false;
        for (Annotation annotation : method.getAnnotations())
            if (annotation.annotationType().equals(Meta.class))
                return true;
        return false;
    }

    private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();
        int TRUSTED = -1;
        return lookupConstructor.newInstance(declaringClass, TRUSTED)
                .unreflectSpecial(method, declaringClass)
                .bindTo(proxy)
                .invokeWithArguments(args);
    }

    @SuppressWarnings("unchecked")
    private Object getValueFor(Method method) {
        String methodName = method.getName();
        Object keywordKey = cachedRead(":" + methodName);
        Object val = GET.invoke(map, keywordKey);
        if (val == null) val = getValueForCustomKey(method);
        if (val == null) return null;

        Class<?> returnType = method.getReturnType();
        if (returnType.equals(int.class) || returnType.equals(Integer.class)) return ((Long) val).intValue();
        if (returnType.equals(float.class) || returnType.equals(Float.class)) return ((Double) val).floatValue();
        if (returnType.equals(short.class) || returnType.equals(Short.class)) return ((Long) val).shortValue();
        if (returnType.equals(byte.class) || returnType.equals(Byte.class)) return ((Long) val).byteValue();

        if (DynamicObject.class.isAssignableFrom(returnType)) return DynamicObject.wrap(val, (Class<T>) returnType);

        if (Set.class.isAssignableFrom(returnType)) return Reification.wrapElements(val, EMPTY_SET);
        if (List.class.isAssignableFrom(returnType)) return Reification.wrapElements(val, EMPTY_VECTOR);
        if (Map.class.isAssignableFrom(returnType)) return Reification.wrapMapElements(val);

        return val;
    }

    private Object getValueForCustomKey(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation.annotationType().equals(Key.class)) {
                String key = ((Key) annotation).value();
                if (key.charAt(0) != ':')
                    key = ":" + key;
                return GET.invoke(map, Clojure.read(key));
            }
        }
        return null;
    }
}
