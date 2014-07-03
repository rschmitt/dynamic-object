package com.github.rschmitt.dynamicobject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

class Reflection {
    static <T extends DynamicObject<T>> Collection<Method> requiredFields(Class<T> type) {
        Collection<Method> fields = fieldGetters(type);
        return fields.stream().filter(Reflection::isRequired).collect(Collectors.toSet());
    }

    static <T extends DynamicObject<T>> Collection<Method> fieldGetters(Class<T> type) {
        Collection<Method> ret = new LinkedHashSet<>();
        for (Method method : type.getDeclaredMethods())
            if (method.getParameterCount() == 0 && !method.isDefault() && !isMetadataGetter(method))
                ret.add(method);
        return ret;
    }

    static boolean isMetadataGetter(Method getter) {
        return getter.getParameterCount() == 0 && hasAnnotation(getter, Meta.class);
    }

    static boolean isRequired(Method getter) {
        return hasAnnotation(getter, Required.class);
    }

    private static boolean hasAnnotation(Method method, Class ann) {
        List<Annotation> annotations = Arrays.asList(method.getAnnotations());
        for (Annotation annotation : annotations)
            if (annotation.annotationType().equals(ann))
                return true;
        return false;
    }

    static boolean isMetadataBuilder(Method method) {
        if (method.getParameterCount() != 1)
            return false;
        Method correspondingGetter = getCorrespondingGetter(method);
        return hasAnnotation(correspondingGetter, Meta.class);
    }

    static String getKeyNameForGetter(Method method) {
        Key annotation = getMethodAnnotation(method, Key.class);
        if (annotation == null)
            return method.getName();
        else
            return annotation.value();
    }

    @SuppressWarnings("unchecked")
    private static <T> T getMethodAnnotation(Method method, Class<T> annotationType) {
        for (Annotation annotation : method.getAnnotations())
            if (annotation.annotationType().equals(annotationType))
                return (T) annotation;
        return null;
    }

    static String getKeyNameForBuilder(Method method) {
        return getKeyNameForGetter(getCorrespondingGetter(method));
    }

    private static Method getCorrespondingGetter(Method builderMethod) {
        try {
            Class<?> type = builderMethod.getDeclaringClass();
            Method correspondingGetter = type.getMethod(builderMethod.getName());
            return correspondingGetter;
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Builder methods must have a corresponding getter method.", ex);
        }
    }

    static Class<?> getRawType(Type type) {
        if (type instanceof Class)
            return (Class<?>) type;
        else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return (Class<?>) parameterizedType.getRawType();
        } else
            throw new UnsupportedOperationException();
    }

    static Type getTypeArgument(Type type, int idx) {
        if (type instanceof Class)
            return Object.class;
        else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return parameterizedType.getActualTypeArguments()[idx];
        } else
            throw new UnsupportedOperationException();
    }
}
