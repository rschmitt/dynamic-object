package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.newInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CollectionsTest {
    @Test
    public void listOfStrings() {
        ListSchema listSchema = deserialize("{:strings [\"one\" \"two\" \"three\"]}", ListSchema.class);
        List<String> stringList = listSchema.strings();
        assertEquals("one", stringList.get(0));
        assertEquals("two", stringList.get(1));
        assertEquals("three", stringList.get(2));
    }

    // This is just here to prove a point about Java<->Clojure interop.
    @Test
    public void listStream() {
        ListSchema listSchema = deserialize("{:strings [\"one\" \"two\" \"three\"]}", ListSchema.class);
        List<String> stringList = listSchema.strings();

        List<Integer> collect = stringList.stream().map(x -> x.length()).collect(Collectors.toList());

        assertEquals(3, collect.get(0).intValue());
        assertEquals(3, collect.get(1).intValue());
        assertEquals(5, collect.get(2).intValue());
    }

    @Test
    public void setOfStrings() {
        SetSchema setSchema = deserialize("{:strings #{\"one\" \"two\" \"three\"}}", SetSchema.class);
        Set<String> stringSet = setSchema.strings();
        assertEquals(3, stringSet.size());
        assertTrue(stringSet.contains("one"));
        assertTrue(stringSet.contains("two"));
        assertTrue(stringSet.contains("three"));
    }

    @Test
    public void embeddedMap() {
        String edn = "{:dictionary {\"key\" \"value\"}}";
        MapSchema mapSchema = deserialize(edn, MapSchema.class);
        assertEquals("value", mapSchema.dictionary().get("key"));
        assertEquals(1, mapSchema.dictionary().size());
    }

    @Test
    public void listOfIntegers() {
        ListSchema deserialized = deserialize("{:ints [nil 2 nil 4 nil]}", ListSchema.class);
        List<Integer> builtList = new ArrayList<>();
        builtList.add(null);
        builtList.add(2);
        builtList.add(null);
        builtList.add(4);
        builtList.add(null);
        ListSchema built = newInstance(ListSchema.class).ints(builtList);

        assertEquals(builtList, deserialized.ints());
        assertEquals(builtList, built.ints());
    }
}

interface ListSchema extends DynamicObject<ListSchema> {
    List<String> strings();
    List<Integer> ints();

    ListSchema ints(List<Integer> ints);
}

interface SetSchema extends DynamicObject<SetSchema> {
    Set<String> strings();
}

interface MapSchema extends DynamicObject<MapSchema> {
    Map<String, String> dictionary();
}
