package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.serialize;
import static org.junit.Assert.*;

public class PrimitiveTest {
    private static final String Edn = "{:i 4, :d 3.14, :f 3.14, :lng 1234567890, :shrt 4, :bool true, :c \\newline, :b 127}";
    private static final Unboxed Unboxed = deserialize(Edn, Unboxed.class);
    private static final Boxed Boxed = deserialize(Edn, Boxed.class);

    @Test
    public void getBoxedFields() {
        assertTrue(4 == Boxed.shrt());
        assertTrue(4 == Boxed.i());
        assertTrue(1234567890L == Boxed.lng());
        assertEquals(3.14, Boxed.d(), 0.001);
        assertEquals(3.14, Boxed.f(), 0.001);
        assertTrue(Boxed.bool());
        assertTrue('\n' == Boxed.c());
        assertTrue((byte) 127 == Boxed.b());
    }

    @Test
    public void getUnboxedFields() {
        assertTrue(4 == Unboxed.shrt());
        assertTrue(4 == Unboxed.i());
        assertTrue(1234567890L == Unboxed.lng());
        assertEquals(3.14, Unboxed.d(), 0.001);
        assertEquals(3.14, Unboxed.f(), 0.001);
        assertTrue(Unboxed.bool());
        assertTrue('\n' == Unboxed.c());
        assertTrue(127 == Unboxed.b());
    }

    @Test
    public void boxedBuilders() {
        Boxed boxed = DynamicObject.newInstance(Boxed.class)
                .i(4)
                .d(3.14)
                .f((float) 3.14)
                .lng(1234567890L)
                .shrt((short) 4)
                .bool(true)
                .c('\n')
                .b((byte) 127);

        assertEquals(Boxed, boxed);
    }

    @Test
    public void unboxedBuilders() {
        Unboxed unboxed = DynamicObject.newInstance(Unboxed.class)
                .i(4)
                .d(3.14)
                .f((float) 3.14)
                .lng(1234567890L)
                .shrt((short) 4)
                .bool(true)
                .c('\n')
                .b((byte)127);

        assertEquals(Unboxed, unboxed);
    }

    @Test
    public void unboxedNullBuilders() {
        String edn = "{:b nil, :c nil, :bool nil, :shrt nil, :lng nil, :f nil, :d nil, :i nil}";
        Boxed boxed = DynamicObject.newInstance(Boxed.class)
                .i(null)
                .d(null)
                .f(null)
                .lng(null)
                .shrt(null)
                .bool(null)
                .c(null)
                .b(null);

        assertEquals(boxed, deserialize(edn, Boxed.class));
        assertEquals(edn, serialize(boxed));
        assertEquals(deserialize(edn, Boxed.class), boxed);
        assertEquals(serialize(boxed), edn);
        assertNull(boxed.shrt());
        assertNull(boxed.i());
        assertNull(boxed.lng());
        assertNull(boxed.f());
        assertNull(boxed.d());
        assertNull(boxed.bool());
        assertNull(boxed.c());
        assertNull(boxed.b());
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
        short shrt();
        int i();
        long lng();
        float f();
        double d();
        boolean bool();
        char c();
        byte b();

        Unboxed shrt(short shrt);
        Unboxed i(int i);
        Unboxed lng(long lng);
        Unboxed f(float f);
        Unboxed d(double d);
        Unboxed bool(boolean bool);
        Unboxed c(char c);
        Unboxed b(byte b);
    }

    public interface Boxed extends DynamicObject<Boxed> {
        Short shrt();
        Integer i();
        Long lng();
        Float f();
        Double d();
        Boolean bool();
        Character c();
        Byte b();

        Boxed shrt(Short shrt);
        Boxed i(Integer i);
        Boxed lng(Long lng);
        Boxed f(Float f);
        Boxed d(Double d);
        Boxed bool(Boolean bool);
        Boxed c(Character c);
        Boxed b(Byte b);
    }
}
