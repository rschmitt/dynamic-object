package com.github.rschmitt.dynamicobject;

import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.Assoc;
import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.Default;
import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.EmptyMap;
import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.ReadString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import clojure.lang.AFn;

public class TestUtils {
    private static final Object readerOpts = Assoc.invoke(EmptyMap, Default, getUnknownReader());

    private static Object getUnknownReader() {
        return new AFn() {
            @Override
            public Object invoke(Object arg1, Object arg2) {
                return new Unknown(arg1.toString(), arg2);
            }
        };
    }

    public static Object genericRead(String str) {
        return ReadString.invoke(readerOpts, str);
    }

    public static void assertEquivalent(String expected, String actual) {
        assertEquals(genericRead(expected), genericRead(actual));
    }

    public static void assertEquivalent(String message, String expected, String actual) {
        assertEquals(genericRead(expected), genericRead(actual), message);
    }
}
