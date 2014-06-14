package com.github.rschmitt.dynamicobject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

class Reflection {
    static <T extends DynamicObject<T>> Collection<Method> fieldGetters(Class<T> type) {
        Collection<Method> ret = new LinkedHashSet<>();
        for (Method method : type.getDeclaredMethods())
            if (method.getParameterCount() == 0 && !method.isDefault() && !isMetadataGetter(method))
                ret.add(method);
        return ret;
    }

    private static boolean isMetadataGetter(Method method) {
        List<Annotation> annotations = Arrays.asList(method.getAnnotations());
        for (Annotation annotation : annotations)
            if (annotation.annotationType().equals(Meta.class))
                return true;
        return false;
    }
}
