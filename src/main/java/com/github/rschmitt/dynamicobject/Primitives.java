package com.github.rschmitt.dynamicobject;

import java.util.*;

/*
 * This class deals with the primitive types that need to be boxed and unboxed.
 */
class Primitives {
    private static final Map<Class<?>, Class<?>> unboxedToBoxed;

    static {
        Map<Class<?>, Class<?>> mapping = new HashMap<>();
        mapping.put(boolean.class, Boolean.class);
        mapping.put(char.class, Character.class);
        mapping.put(byte.class, Byte.class);
        mapping.put(short.class, Short.class);
        mapping.put(int.class, Integer.class);
        mapping.put(long.class, Long.class);
        mapping.put(float.class, Float.class);
        mapping.put(double.class, Double.class);
        unboxedToBoxed = mapping;
    }

    static Class<?> box(Class<?> type) {
        return unboxedToBoxed.getOrDefault(type, type);
    }
}
