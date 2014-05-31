package com.github.rschmitt.dynamicobject;

import clojure.lang.PersistentHashMap;
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
    }

    @Test
    public void invokeBuilderWithPrimitive() {
        Buildable obj = DynamicObject.newInstance(Buildable.class).i(4);
        assertEquals("{:i 4}", DynamicObject.serialize(obj));
    }

    @Test
    public void invokeBuilderWithNull() {
        Buildable obj = DynamicObject.newInstance(Buildable.class).str(null);
        assertEquals("{:str nil}", DynamicObject.serialize(obj));
    }
}


interface Buildable extends DynamicObject<Buildable> {
    String str();

    Buildable str(String str);

    int i();

    Buildable i(int i);
}
