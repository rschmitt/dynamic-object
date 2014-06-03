package com.github.rschmitt.dynamicobject;

import clojure.lang.*;
import org.junit.Test;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.serialize;
import static java.lang.String.format;
import static org.junit.Assert.*;

public class MapTest {
    static final String SIMPLE_EDN = "{:str \"expected value\", :i 4, :d 3.14}";
    static final String NESTED_EDN = format("{:version 1, :simple %s}", SIMPLE_EDN);

    @Test
    public void getMapReturnsBackingMap() {
        EmptyObject object = deserialize(NESTED_EDN, EmptyObject.class);
        Object map = EdnReader.readString(NESTED_EDN, PersistentHashMap.EMPTY);
        assertEquals(map, object.getMap());
    }

    @Test
    public void unknownFieldsAreConsideredForEquality() {
        EmptyObject obj1 = deserialize(SIMPLE_EDN, EmptyObject.class);
        EmptyObject obj2 = deserialize(NESTED_EDN, EmptyObject.class);
        assertFalse(obj1.equals(obj2));
    }

    @Test
    public void unknownFieldsAreSerialized() {
        EmptyObject nestedObj = deserialize(NESTED_EDN, EmptyObject.class);
        String actualEdn = serialize(nestedObj);
        assertEquals(NESTED_EDN, actualEdn);
    }

    public interface EmptyObject extends DynamicObject<EmptyObject> {
    }
}
