package com.github.rschmitt.dynamicobject;

import java.math.BigInteger;
import java.util.*;

import static com.github.rschmitt.dynamicobject.ClojureStuff.BIGINT;
import static com.github.rschmitt.dynamicobject.ClojureStuff.BIGINTEGER;

/*
 * This class deals with the numeric types that need to be converted to and from long/double/clojure.lang.BigInt.
 */
public class Numerics {
    private static final Set<Class<?>> numericTypes;
    private static final Map<Class<?>, Class<?>> numericConversions;
    private static final Class<?> BigInt = BIGINT.invoke(0).getClass();

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
        types.add(BigInteger.class);
        numericTypes = Collections.unmodifiableSet(types);

        Map<Class<?>, Class<?>> conversions = new HashMap<>();
        conversions.put(Byte.class, Long.class);
        conversions.put(Short.class, Long.class);
        conversions.put(Integer.class, Long.class);
        conversions.put(Float.class, Double.class);
        conversions.put(BigInteger.class, BigInt);
        numericConversions = conversions;
    }

    static boolean isNumeric(Class<?> type) {
        return numericTypes.contains(type);
    }

    static Class<?> canonicalNumericType(Class<?> type) {
        return numericConversions.getOrDefault(type, type);
    }

    static Object maybeDownconvert(Class type, Object val) {
        if (val == null) return null;
        if (type.equals(int.class) || type.equals(Integer.class)) return ((Long) val).intValue();
        if (type.equals(float.class) || type.equals(Float.class)) return ((Double) val).floatValue();
        if (type.equals(short.class) || type.equals(Short.class)) return ((Long) val).shortValue();
        if (type.equals(byte.class) || type.equals(Byte.class)) return ((Long) val).byteValue();
        if (type.equals(BigInt)) return BIGINTEGER.invoke(val);
        return val;
    }

    static Object maybeUpconvert(Object val) {
        if (val instanceof Float) return Double.parseDouble(String.valueOf(val));
        else if (val instanceof Short) return (long) ((short) val);
        else if (val instanceof Byte) return (long) ((byte) val);
        else if (val instanceof Integer) return (long) ((int) val);
        else if (val instanceof BigInteger) return BIGINT.invoke(val);
        return val;
    }
}
