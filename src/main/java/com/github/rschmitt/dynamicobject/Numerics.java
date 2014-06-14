package com.github.rschmitt.dynamicobject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/*
 * This class deals with the numeric types that need to be converted to and from long/double/clojure.lang.BigInt.
 */
public class Numerics {
    private static final Set<Class<?>> numericTypes;

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
    }

    static boolean isNumeric(Class<?> type) {
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
