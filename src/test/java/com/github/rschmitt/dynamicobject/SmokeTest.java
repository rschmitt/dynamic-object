package com.github.rschmitt.dynamicobject;

import clojure.lang.*;
import org.junit.Test;

import static java.lang.String.format;
import static org.junit.Assert.*;

public class SmokeTest {
    static final String SIMPLE_SCHEMA_EDN = "{:str \"expected value\", :i 4, :d 3.14}";
    static final String NESTED_SCHEMA_EDN = format("{:version 1, :simple %s}", SIMPLE_SCHEMA_EDN);

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
        NestedSchema nestedSchema = DynamicObject.deserialize(NESTED_SCHEMA_EDN, NestedSchema.class);
        IPersistentMap map = (IPersistentMap) EdnReader.readString(NESTED_SCHEMA_EDN, PersistentHashMap.EMPTY);
        assertEquals(map, nestedSchema.getMap());
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
        int i();

        double d();

        String str();
    }

    public interface NestedSchema extends DynamicObject<NestedSchema> {
        int version();

        SimpleSchema simple();
    }
}
