package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.serialize;
import static com.github.rschmitt.dynamicobject.TestUtils.assertEquivalent;
import static org.junit.Assert.*;

public class PrimitiveTest {
    private static final String Edn = "{:d 3.14, :lng 1234567890, :bool true, :c \\newline}";
    private static final Unboxed Unboxed = deserialize(Edn, Unboxed.class);
    private static final Boxed Boxed = deserialize(Edn, Boxed.class);

    @Test
    public void getBoxedFields() {
        assertTrue(1234567890L == Boxed.lng());
        assertEquals(3.14, Boxed.d(), 0.001);
        assertTrue(Boxed.bool());
        assertTrue('\n' == Boxed.c());
    }

    @Test
    public void getUnboxedFields() {
        assertTrue(1234567890L == Unboxed.lng());
        assertEquals(3.14, Unboxed.d(), 0.001);
        assertTrue(Unboxed.bool());
        assertTrue('\n' == Unboxed.c());
    }

    @Test
    public void boxedBuilders() {
        Boxed boxed = DynamicObject.newInstance(Boxed.class)
                .d(3.14)
                .lng(1234567890L)
                .bool(true)
                .c('\n');

        assertEquals(Boxed, boxed);
    }

    @Test
    public void unboxedBuilders() {
        Unboxed unboxed = DynamicObject.newInstance(Unboxed.class)
                .d(3.14)
                .lng(1234567890L)
                .bool(true)
                .c('\n');

        assertEquals(Unboxed, unboxed);
    }

    @Test
    public void unboxedNullBuilders() {
        String edn = "{:c nil, :bool nil, :lng nil, :d nil}";
        Boxed boxed = DynamicObject.newInstance(Boxed.class)
                .d(null)
                .lng(null)
                .bool(null)
                .c(null);

        assertEquals(boxed, deserialize(edn, Boxed.class));
        assertEquivalent(edn, serialize(boxed));
        assertEquals(deserialize(edn, Boxed.class), boxed);
        assertEquivalent(serialize(boxed), edn);
        assertNull(boxed.lng());
        assertNull(boxed.d());
        assertNull(boxed.bool());
        assertNull(boxed.c());
    }

    @Test
    public void unboxedRoundTrip() {
        Unboxed unboxed = deserialize(Edn, Unboxed.class);
        assertEquals(Edn, unboxed.toString());
    }

    @Test
    public void boxedRoundTrip() {
        Boxed boxed = deserialize(Edn, Boxed.class);
        assertEquals(Edn, boxed.toString());
    }

    @Test
    public void testEquality() {
        assertEquals(Boxed, Unboxed);
    }

    public interface Unboxed extends DynamicObject<Unboxed> {
        long lng();
        double d();
        boolean bool();
        char c();

        Unboxed lng(long lng);
        Unboxed d(double d);
        Unboxed bool(boolean bool);
        Unboxed c(char c);
    }

    public interface Boxed extends DynamicObject<Boxed> {
        Long lng();
        Double d();
        Boolean bool();
        Character c();

        Boxed lng(Long lng);
        Boxed d(Double d);
        Boxed bool(Boolean bool);
        Boxed c(Character c);
    }
}
