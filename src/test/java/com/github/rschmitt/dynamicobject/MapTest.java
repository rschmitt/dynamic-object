package com.github.rschmitt.dynamicobject;

import clojure.lang.EdnReader;
import clojure.lang.PersistentHashMap;
import org.junit.Test;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.serialize;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MapTest {
    static final String SimpleEdn = "{:str \"expected value\", :i 4, :d 3.14}";
    static final String NestedEdn = format("{:version 1, :simple %s}", SimpleEdn);

    @Test
    public void getMapReturnsBackingMap() {
        EmptyObject object = deserialize(NestedEdn, EmptyObject.class);
        Object map = EdnReader.readString(NestedEdn, PersistentHashMap.EMPTY);
        assertEquals(map, object.getMap());
    }

    @Test
    public void unknownFieldsAreConsideredForEquality() {
        EmptyObject obj1 = deserialize(SimpleEdn, EmptyObject.class);
        EmptyObject obj2 = deserialize(NestedEdn, EmptyObject.class);
        assertFalse(obj1.equals(obj2));
    }

    @Test
    public void unknownFieldsAreSerialized() {
        EmptyObject nestedObj = deserialize(NestedEdn, EmptyObject.class);
        String actualEdn = serialize(nestedObj);
        assertEquals(NestedEdn, actualEdn);
    }

    public interface EmptyObject extends DynamicObject<EmptyObject> {
    }
}
