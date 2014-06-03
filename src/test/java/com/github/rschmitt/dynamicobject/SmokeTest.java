package com.github.rschmitt.dynamicobject;

import clojure.lang.*;
import org.junit.Test;

import static java.lang.String.format;
import static org.junit.Assert.*;

public class SmokeTest {
    static final String SIMPLE_SCHEMA_EDN = "{:str \"expected value\", :i 4, :d 3.14, :f 3.14, :lng 1234567890, :shrt 4, :b true}";
    static final String NESTED_SCHEMA_EDN = format("{:version 1, :simple %s}", SIMPLE_SCHEMA_EDN);

    @Test
    public void getSimpleFields() {
        SimpleSchema simpleSchema = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);

        assertEquals("expected value", simpleSchema.str());
        assertEquals(4, simpleSchema.shrt());
        assertEquals(4, simpleSchema.i());
        assertEquals(1234567890L, simpleSchema.lng());
        assertEquals(3.14, simpleSchema.d(), 0.001);
        assertEquals(3.14, simpleSchema.f(), 0.001);
        assertTrue(simpleSchema.b());
    }

    @Test
    public void nesting() {
        SimpleSchema simpleSchema = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);
        NestedSchema nestedSchema = DynamicObject.deserialize(NESTED_SCHEMA_EDN, NestedSchema.class);

        assertEquals(1, nestedSchema.version());
        assertEquals(simpleSchema, nestedSchema.simple());
    }

    @Test
    public void basicRoundTrip() {
        SimpleSchema simpleSchema = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);

        assertEquals(SIMPLE_SCHEMA_EDN, simpleSchema.toString());
    }

    @Test
    public void getMap() {
        SimpleSchema simpleSchema = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);
        IPersistentMap map = (IPersistentMap) EdnReader.readString(SIMPLE_SCHEMA_EDN, PersistentHashMap.EMPTY);
        assertEquals(map, simpleSchema.getMap());
    }

    @Test
    public void unknownFields() {
        String edn = "{:str \"str\", :i 4, :d 3.14, :unknown \"unknown\"}";

        SimpleSchema withUnknowns = DynamicObject.deserialize(edn, SimpleSchema.class);
        SimpleSchema regular = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);

        assertFalse(withUnknowns.equals(regular));
        assertEquals(edn, withUnknowns.toString());
    }

    public interface SimpleSchema extends DynamicObject<SimpleSchema> {
        short shrt();

        int i();

        long lng();

        float f();

        double d();

        String str();

        boolean b();
    }

    public interface NestedSchema extends DynamicObject<NestedSchema> {
        int version();

        SimpleSchema simple();
    }
}
