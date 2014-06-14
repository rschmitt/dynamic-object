package com.github.rschmitt.dynamicobject;

import java.util.*;

/*
 * This class deals with the primitive numeric types that need to be converted to and from long and double.
 */
class Primitives {
    private static final Set<Class<?>> numericTypes;
    private static final Map<Class<?>, Class<?>> unboxedToBoxed;

    static {
        Set<Class<?>> types = new HashSet<>();
        types.add(int.class);
        types.add(Integer.class);
        types.add(float.class);
        types.add(Float.class);
        types.add(short.class);
        types.add(Short.class);
        types.add(byte.class);
        types.add(Byte.class);
        numericTypes = Collections.unmodifiableSet(types);

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

    static boolean isPrimitive(Class type) {
        return numericTypes.contains(type);
    }

    static Object maybeDownconvert(Class type, Object val) {
        if (val == null) return null;
        if (type.equals(int.class) || type.equals(Integer.class)) return ((Long) val).intValue();
        if (type.equals(float.class) || type.equals(Float.class)) return ((Double) val).floatValue();
        if (type.equals(short.class) || type.equals(Short.class)) return ((Long) val).shortValue();
        if (type.equals(byte.class) || type.equals(Byte.class)) return ((Long) val).byteValue();
        return val;
    }

    static Object maybeUpconvert(Object val) {
        if (val instanceof Float) return Double.parseDouble(String.valueOf(val));
        else if (val instanceof Short) return (long) ((short) val);
        else if (val instanceof Byte) return (long) ((byte) val);
        else if (val instanceof Integer) return (long) ((int) val);
        return val;
    }
}
