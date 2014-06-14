package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.AFn;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.rschmitt.dynamicobject.ClojureStuff.*;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

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
            Object val = Primitives.maybeUpconvert(args[0]);
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
            case "union":
                return union((DynamicObject<T>) args[0]);
            case "subtract":
                return subtract((DynamicObject<T>) args[0]);
            case "validate":
                validate();
                return null;
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
        union = Erasure.withTypeMetadata(union, type);
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
                }
            } catch (ClassCastException cce) {
                mismatchedFields.put(field, getRawValueFor(field).getClass());
            }
        }
        if (!missingFields.isEmpty() || !mismatchedFields.isEmpty())
            throw new IllegalStateException(getValidationErrorMessage(missingFields, mismatchedFields));
    }

    private static String getValidationErrorMessage(Collection<Method> missingFields, Map<Method, Class<?>> mismatchedFields) {
        StringBuilder ret = new StringBuilder();
        if (!missingFields.isEmpty()) {
            ret.append("The following @Required fields were missing: ");
            List<String> fieldNames = missingFields.stream().map(Method::getName).collect(toList());
            for (int i = 0; i < fieldNames.size(); i++) {
                ret.append(fieldNames.get(i));
                if (i != fieldNames.size() - 1)
                    ret.append(", ");
            }
            ret.append("\n");
        }
        if (!mismatchedFields.isEmpty()) {
            ret.append("The following fields had the wrong type:\n");
            for (Map.Entry<Method, Class<?>> methodClassEntry : mismatchedFields.entrySet()) {
                Method method = methodClassEntry.getKey();
                String name = method.getName();
                String expected = method.getReturnType().getSimpleName();
                String actual = methodClassEntry.getValue().getSimpleName();
                ret.append(format("\t%s (expected %s, got %s)%n", name, expected, actual));
            }
        }
        return ret.toString();
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
        Class<?> returnType = method.getReturnType();
        Type genericReturnType = method.getGenericReturnType();
        return maybeConvertValue(val, returnType, genericReturnType);
    }

    private Object getRawValueFor(Method method) {
        String methodName = method.getName();
        Object keywordKey = cachedRead(":" + methodName);
        Object val = GET.invoke(map, keywordKey);
        if (val == null) val = getValueForCustomKey(method);
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

    @SuppressWarnings("unchecked")
    private Object maybeConvertValue(Object val, Class<?> returnType, Type genericReturnType) {
        if (Primitives.isPrimitive(returnType)) return Primitives.maybeDownconvert(returnType, val);

        if (DynamicObject.class.isAssignableFrom(returnType)) return DynamicObject.wrap(val, (Class<T>) returnType);

        if (Set.class.isAssignableFrom(returnType)) return Reification.wrapElements(val, EMPTY_SET, genericReturnType);
        if (List.class.isAssignableFrom(returnType)) return Reification.wrapElements(val, EMPTY_VECTOR, genericReturnType);
        if (Map.class.isAssignableFrom(returnType)) return Reification.wrapMapElements(val, genericReturnType);

        return val;
    }
}
