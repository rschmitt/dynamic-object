package com.github.rschmitt.dynamicobject;

import clojure.lang.AFn;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
            if (Reflection.isMetadataBuilder(method))
                return assocMeta(methodName, args[0]);
            String key = Reflection.getKeyNameForBuilder(method);
            return assoc(key, Conversions.javaToClojure(args[0]));
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
            case "intersect":
                return union((DynamicObject<T>) args[0]);
            case "subtract":
                return subtract((DynamicObject<T>) args[0]);
            case "validate":
                validate();
                return proxy;
            case "equals":
                Object other = args[0];
                if (other instanceof DynamicObject)
                    return map.equals(((DynamicObject) other).getMap());
                else
                    return method.invoke(map, args);
            default:
                if (Reflection.isMetadataGetter(method))
                    return getMetadataFor(methodName);
                return getAndCacheValueFor(method);
        }
    }

    private Object union(DynamicObject<T> arg) {
        return diff(arg, 2);
    }

    private Object subtract(DynamicObject<T> arg) {
        return diff(arg, 0);
    }

    private Object diff(DynamicObject<T> arg, int idx) {
        Object array = DIFF.invoke(map, arg.getMap());
        Object union = NTH.invoke(array, idx);
        if (union == null) union = EMPTY_MAP;
        union = Metadata.withTypeMetadata(union, type);
        return DynamicObject.wrap(union, type);
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

    private void validate() {
        Collection<Method> fields = Reflection.fieldGetters(type);
        Collection<Method> missingFields = new LinkedHashSet<>();
        Map<Method, Class<?>> mismatchedFields = new HashMap<>();
        for (Method field : fields) {
            try {
                Object val = getAndCacheValueFor(field);
                if (Reflection.isRequired(field) && val == null)
                    missingFields.add(field);
                if (val != null) {
                    Class<?> expectedType = Primitives.box(field.getReturnType());
                    Class<?> actualType = val.getClass();
                    if (!expectedType.isAssignableFrom(actualType))
                        mismatchedFields.put(field, actualType);
                    if (val instanceof DynamicObject)
                        ((DynamicObject) val).validate();
                    else if (val instanceof List || val instanceof Set)
                        Validation.validateCollection((Collection<?>) val, field.getGenericReturnType());
                    else if (val instanceof Map)
                        Validation.validateMap((Map<?, ?>) val, field.getGenericReturnType());
                }
            } catch (ClassCastException | AssertionError cce) {
                mismatchedFields.put(field, getRawValueFor(field).getClass());
            }
        }
        if (!missingFields.isEmpty() || !mismatchedFields.isEmpty())
            throw new IllegalStateException(Validation.getValidationErrorMessage(missingFields, mismatchedFields));
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

    private T assoc(String key, Object value) {
        if (value instanceof DynamicObject)
            value = ((DynamicObject) value).getMap();
        return DynamicObject.wrap(ASSOC.invoke(map, getMapKey(key), value), type);
    }

    private Object assocMeta(String key, Object value) {
        return DynamicObject.wrap(VARY_META.invoke(map, ASSOC, key, value), type);
    }

    private boolean isBuilderMethod(Method method) {
        return method.getReturnType().equals(type) && method.getParameterCount() == 1;
    }

    private Object getMetadataFor(String key) {
        Object meta = META.invoke(map);
        return GET.invoke(meta, key);
    }

    private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();
        int TRUSTED = -1;
        return lookupConstructor.newInstance(declaringClass, TRUSTED)
                .unreflectSpecial(method, declaringClass)
                .bindTo(proxy)
                .invokeWithArguments(args);
    }

    private Object getValueFor(Method method) {
        Object val = getRawValueFor(method);
        if (val == null) return null;
        Type genericReturnType = method.getGenericReturnType();
        return Conversions.clojureToJava(val, genericReturnType);
    }

    private Object getRawValueFor(Method method) {
        String keyName = Reflection.getKeyNameForGetter(method);
        Object keywordKey = getMapKey(keyName);
        return GET.invoke(map, keywordKey);
    }

    private static Object getMapKey(String keyName) {
        if (keyName.charAt(0) == ':')
            keyName = keyName.substring(1);
        return cachedRead(":" + keyName);
    }
}
