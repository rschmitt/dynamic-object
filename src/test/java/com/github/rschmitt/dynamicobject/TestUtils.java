package com.github.rschmitt.dynamicobject;

import clojure.lang.AFn;

import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.*;
import static org.junit.Assert.assertEquals;

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
        assertEquals(message, genericRead(expected), genericRead(actual));
    }
}
