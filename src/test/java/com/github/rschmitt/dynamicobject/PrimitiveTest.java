package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PrimitiveTest {
    private static final String EDN = "{:i 4, :d 3.14, :f 3.14, :lng 1234567890, :shrt 4, :bool true, :c \\newline, :b 127}";
    private static final Unboxed UNBOXED = DynamicObject.deserialize(EDN, Unboxed.class);
    private static final Boxed BOXED = DynamicObject.deserialize(EDN, Boxed.class);

    @Test
    public void getBoxedFields() {
        assertTrue(4 == BOXED.shrt());
        assertTrue(4 == BOXED.i());
        assertTrue(1234567890L == BOXED.lng());
        assertEquals(3.14, BOXED.d(), 0.001);
        assertEquals(3.14, BOXED.f(), 0.001);
        assertTrue(BOXED.bool());
        assertTrue('\n' == BOXED.c());
        assertTrue((byte) 127 == BOXED.b());
    }

    @Test
    public void getUnboxedFields() {
        assertTrue(4 == UNBOXED.shrt());
        assertTrue(4 == UNBOXED.i());
        assertTrue(1234567890L == UNBOXED.lng());
        assertEquals(3.14, UNBOXED.d(), 0.001);
        assertEquals(3.14, UNBOXED.f(), 0.001);
        assertTrue(UNBOXED.bool());
        assertTrue('\n' == UNBOXED.c());
        assertTrue(127 == UNBOXED.b());
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

        assertEquals(BOXED, boxed);
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

        assertEquals(UNBOXED, unboxed);
    }

    @Test
    public void unboxedRoundTrip() {
        Unboxed unboxed = DynamicObject.deserialize(EDN, Unboxed.class);
        assertEquals(EDN, unboxed.toString());
    }

    @Test
    public void boxedRoundTrip() {
        Boxed boxed = DynamicObject.deserialize(EDN, Boxed.class);
        assertEquals(EDN, boxed.toString());
    }

    @Test
    public void testEquality() {
        assertEquals(BOXED, UNBOXED);
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
