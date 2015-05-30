package com.github.rschmitt.dynamicobject;

import clojure.lang.PersistentHashMap;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BuilderTest {
    @Test
    public void createEmptyInstance() {
        Buildable obj = DynamicObject.newInstance(Buildable.class);
        assertEquals(PersistentHashMap.EMPTY, obj.getMap());
        assertEquals("{}", DynamicObject.serialize(obj));
    }

    @Test
    public void invokeBuilderMethod() {
        Buildable obj = DynamicObject.newInstance(Buildable.class).str("string");
        assertEquals("{:str \"string\"}", DynamicObject.serialize(obj));
        assertEquals("string", obj.str());
    }

    @Test
    @Ignore("Breaks on Clojure 1.7.0-RC1")
    public void invokeBuilderWithPrimitive() {
        Buildable obj = DynamicObject.newInstance(Buildable.class).i(4).s((short) 127).l(Long.MAX_VALUE).d(3.14).f((float) 3.14);
        assertEquals("{:f 3.14, :d 3.14, :l 9223372036854775807, :s 127, :i 4}", DynamicObject.serialize(obj));
        assertEquals(4, obj.i());
        assertEquals(127, obj.s());
        assertEquals(Long.MAX_VALUE, obj.l());
        assertEquals(3.14, obj.f(), 0.00001);
        assertEquals(3.14, obj.d(), 0.00001);
    }

    @Test
    public void invokeBuilderWithNull() {
        Buildable obj = DynamicObject.newInstance(Buildable.class).str(null);
        assertEquals("{:str nil}", DynamicObject.serialize(obj));
    }

    public interface Buildable extends DynamicObject<Buildable> {
        String str();
        int i();
        long l();
        short s();
        float f();
        double d();

        Buildable str(String str);
        Buildable i(int i);
        Buildable l(long l);
        Buildable s(short s);
        Buildable f(float f);
        Buildable d(double d);
    }
}
