package com.github.rschmitt.dynamicobject;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.serialize;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import clojure.lang.EdnReader;
import clojure.lang.PersistentHashMap;

import java.util.HashMap;

public class MapTest {
    static final String SimpleEdn = "{:str \"expected value\", :i 4, :d 3.14}";
    static final String NestedEdn = format("{:version 1, :simple %s}", SimpleEdn);
    static final String TaggedEdn = format("#eo%s", NestedEdn);

    @Before
    public void setup() {
        DynamicObject.registerTag(EmptyObject.class, "eo");
    }

    @After
    public void teardown() {
        DynamicObject.deregisterTag(EmptyObject.class);
    }

    @Test
    public void getMapReturnsBackingMap() {
        EmptyObject object = deserialize(TaggedEdn, EmptyObject.class);
        Object map = EdnReader.readString(NestedEdn, PersistentHashMap.EMPTY);
        assertEquals(map, object.getMap());
        binaryRoundTrip(object);
    }

    @Test
    public void unknownFieldsAreConsideredForEquality() {
        EmptyObject obj1 = deserialize(SimpleEdn, EmptyObject.class);
        EmptyObject obj2 = deserialize(NestedEdn, EmptyObject.class);
        assertFalse(obj1.equals(obj2));
        binaryRoundTrip(obj1);
        binaryRoundTrip(obj2);
    }

    @Test
    public void unknownFieldsAreSerialized() {
        EmptyObject nestedObj = deserialize(TaggedEdn, EmptyObject.class);
        String actualEdn = serialize(nestedObj);
        assertEquals(TaggedEdn, actualEdn);
    }

    @Test
    public void mapDefaultMethodsAreUsable() throws Exception {
        EmptyObject object = DynamicObject.newInstance(EmptyObject.class);

        object.getOrDefault("some key", "some value");
    }

    @Test
    public void wrappedMapGettersWork() throws Exception {
        HashMap<Object, Object> map = new HashMap<>();
        map.put("foo", "bar");

        TestObject obj = DynamicObject.wrap(map, TestObject.class);

        assertEquals("bar", obj.foo());
    }


    @Test
    public void wrappedMapSettersWork() throws Exception {
        HashMap<Object, Object> map = new HashMap<>();
        map.put("foo", "bar");

        TestObject obj = DynamicObject.wrap(map, TestObject.class);
        obj = obj.withFoo("quux");

        assertEquals("quux", obj.foo());
    }

    @Test
    public void wrappedMapMetaWorks() throws Exception {
        HashMap<Object, Object> map = new HashMap<>();
        map.put("foo", "bar");

        TestObject obj = DynamicObject.wrap(map, TestObject.class);

        assertNull(obj.getMeta());
        obj = obj.withMeta("x");
        assertEquals("x", obj.getMeta());

        assertEquals(1, obj.getMap().size());
    }

    private void binaryRoundTrip(Object expected) {
        Object actual = DynamicObject.fromFressianByteArray(DynamicObject.toFressianByteArray(expected));
        assertEquals(expected, actual);
    }

    public interface EmptyObject extends DynamicObject<EmptyObject> {
    }

    public interface TestObject extends DynamicObject<TestObject> {
        @Key("foo") String foo();
        @Key("foo") TestObject withFoo(String foo);

        @Meta @Key(":meta") String getMeta();
        @Meta @Key(":meta") TestObject withMeta(String meta);
    }
}
