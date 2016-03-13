package com.github.rschmitt.dynamicobject;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.serialize;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import clojure.lang.EdnReader;
import clojure.lang.PersistentHashMap;

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

    private void binaryRoundTrip(Object expected) {
        Object actual = DynamicObject.fromFressianByteArray(DynamicObject.toFressianByteArray(expected));
        assertEquals(expected, actual);
    }

    public interface EmptyObject extends DynamicObject<EmptyObject> {
    }
}
