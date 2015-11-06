package com.github.rschmitt.dynamicobject.internal;

import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.cachedRead;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import com.github.rschmitt.dynamicobject.*;

class Reflection {
    static <D extends DynamicObject<D>> Collection<Method> requiredFields(Class<D> type) {
        Collection<Method> fields = fieldGetters(type);
        return fields.stream().filter(Reflection::isRequired).collect(Collectors.toSet());
    }

    static <D extends DynamicObject<D>> Set<Object> cachedKeys(Class<D> type) {
        return Arrays.asList(type.getMethods()).stream()
                .filter(method -> method.getAnnotation(Cached.class) != null)
                .map(method -> method.getAnnotation(Key.class))
                .filter(key -> key != null)
                .map(Key::value)
                .map(Reflection::stringToKey)
                .collect(Collectors.toSet());
    }

    static <D extends DynamicObject<D>> Collection<Method> fieldGetters(Class<D> type) {
        Collection<Method> ret = new LinkedHashSet<>();
        for (Method method : type.getDeclaredMethods())
            if (method.getParameterCount() == 0 && !method.isDefault() && !method.isSynthetic() && !isMetadataGetter(method))
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
        if (hasAnnotation(method, Meta.class))
            return true;
        if (hasAnnotation(method, Key.class))
            return false;
        Method correspondingGetter = getCorrespondingGetter(method);
        return hasAnnotation(correspondingGetter, Meta.class);
    }

    static Object getKeyForGetter(Method method) {
        Key annotation = getMethodAnnotation(method, Key.class);
        if (annotation == null)
            return stringToKey(":" + method.getName());
        else
            return stringToKey(annotation.value());
    }

    private static Object stringToKey(String keyName) {
        if (keyName.charAt(0) == ':')
            return cachedRead(keyName);
        else
            return keyName;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getMethodAnnotation(Method method, Class<T> annotationType) {
        for (Annotation annotation : method.getAnnotations())
            if (annotation.annotationType().equals(annotationType))
                return (T) annotation;
        return null;
    }

    static Object getKeyForBuilder(Method method) {
        Key annotation = getMethodAnnotation(method, Key.class);
        if (annotation == null)
            return getKeyForGetter(getCorrespondingGetter(method));
        else
            return stringToKey(annotation.value());
    }

    private static Method getCorrespondingGetter(Method builderMethod) {
        try {
            Class<?> type = builderMethod.getDeclaringClass();
            Method correspondingGetter = type.getMethod(builderMethod.getName());
            return correspondingGetter;
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Builder methods must have a corresponding getter method or a @Key annotation.", ex);
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
