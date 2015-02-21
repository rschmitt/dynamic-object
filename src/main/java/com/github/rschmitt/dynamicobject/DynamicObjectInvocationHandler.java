package com.github.rschmitt.dynamicobject;

import clojure.lang.AFn;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.rschmitt.dynamicobject.ClojureStuff.*;
import static com.github.rschmitt.dynamicobject.ClojureStuff.Meta;
import static java.lang.String.format;

class DynamicObjectInvocationHandler<T extends DynamicObject<T>> implements InvocationHandler {
    private static final Object Default = new Object();
    private static final Object Null = new Object();

    private final Object map;
    private final Class<T> type;
    private final ConcurrentHashMap valueCache = new ConcurrentHashMap();

    DynamicObjectInvocationHandler(Object map, Class<T> type) {
        this.map = map;
        this.type = type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if (method.isDefault()) {
            if (methodName.equals("validate"))
                Validation.validateInstance(type, this::getAndCacheValueFor, this::getRawValueFor);
            return invokeDefaultMethod(proxy, method, args);
        }

        if (isBuilderMethod(method))
            return invokeBuilderMethod(method, args);

        switch (methodName) {
            case "getMap": return map;
            case "getType": return type;
            case "toString": return map.toString();
            case "hashCode": return map.hashCode();
            case "prettyPrint": return Pprint.invoke(map);
            case "toFormattedString":
                Writer w = new StringWriter();
                Pprint.invoke(map, w);
                return w.toString();
            case "merge": return merge((DynamicObject<T>) args[0]);
            case "intersect": return intersect((DynamicObject<T>) args[0]);
            case "subtract": return subtract((DynamicObject<T>) args[0]);
            case "validate":
                Validation.validateInstance(type, this::getAndCacheValueFor, this::getRawValueFor);
                return proxy;
            case "equals":
                Object other = args[0];
                if (other instanceof DynamicObject)
                    return map.equals(((DynamicObject) other).getMap());
                else
                    return method.invoke(map, args);
            default:
                return invokeGetterMethod(method);
        }
    }

    private Object invokeBuilderMethod(Method method, Object[] args) {
        if (Reflection.isMetadataBuilder(method))
            return assocMeta(method.getName(), args[0]);
        Object key = Reflection.getKeyForBuilder(method);
        return assoc(key, Conversions.javaToClojure(args[0]));
    }

    private Object invokeGetterMethod(Method method) {
        String methodName = method.getName();
        if (Reflection.isMetadataGetter(method))
            return getMetadataFor(methodName);
        Object value = getAndCacheValueFor(method);
        if (value == null && Reflection.isRequired(method))
            throw new NullPointerException(format("Required field %s was null", methodName));
        return value;
    }

    private Object intersect(DynamicObject<T> arg) {
        return diff(arg, 2);
    }

    private Object subtract(DynamicObject<T> arg) {
        return diff(arg, 0);
    }

    private Object diff(DynamicObject<T> arg, int idx) {
        Object array = Diff.invoke(map, arg.getMap());
        Object union = Nth.invoke(array, idx);
        if (union == null) union = EmptyMap;
        union = Metadata.withTypeMetadata(union, type);
        return DynamicObject.wrap(union, type);
    }

    private T merge(DynamicObject<T> other) {
        AFn ignoreNulls = new AFn() {
            public Object invoke(Object arg1, Object arg2) {
                return (arg2 == null) ? arg1 : arg2;
            }
        };
        Object mergedMap = MergeWith.invoke(ignoreNulls, map, other.getMap());
        return DynamicObject.wrap(mergedMap, type);
    }

    @SuppressWarnings("unchecked")
    private Object getAndCacheValueFor(Method method) {
        Object cachedValue = valueCache.getOrDefault(method, Default);
        if (cachedValue == Null) return null;
        if (cachedValue != Default) return cachedValue;
        Object value = getValueFor(method);
        if (value == null)
            valueCache.putIfAbsent(method, Null);
        else
            valueCache.putIfAbsent(method, value);
        return value;
    }

    private T assoc(Object key, Object value) {
        if (value instanceof DynamicObject)
            value = ((DynamicObject) value).getMap();
        return DynamicObject.wrap(Assoc.invoke(map, key, value), type);
    }

    private Object assocMeta(String key, Object value) {
        return DynamicObject.wrap(VaryMeta.invoke(map, Assoc, key, value), type);
    }

    private boolean isBuilderMethod(Method method) {
        return method.getReturnType().equals(type) && method.getParameterCount() == 1;
    }

    private Object getMetadataFor(String key) {
        Object meta = Meta.invoke(map);
        return Get.invoke(meta, key);
    }

    private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();
        Constructor<MethodHandles.Lookup> lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
        lookupConstructor.setAccessible(true);
        int TRUSTED = -1;
        return lookupConstructor.newInstance(declaringClass, TRUSTED)
                .unreflectSpecial(method, declaringClass)
                .bindTo(proxy)
                .invokeWithArguments(args);
    }

    private Object getValueFor(Method method) {
        Object val = getRawValueFor(method);
        Type genericReturnType = method.getGenericReturnType();
        return Conversions.clojureToJava(val, genericReturnType);
    }

    private Object getRawValueFor(Method method) {
        Object key = Reflection.getKeyForGetter(method);
        return Get.invoke(map, key);
    }
}
