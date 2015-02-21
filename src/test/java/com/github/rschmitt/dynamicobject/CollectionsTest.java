package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.newInstance;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CollectionsTest {
    private static final Random Random = new Random();
    private static final Base64.Encoder Encoder = Base64.getEncoder();

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

        List<Integer> collect = stringList.stream().map(x -> x.length()).collect(toList());

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
        List<Integer> builtList = asList(null, 2, null, 4, null);

        ListSchema built = newInstance(ListSchema.class).ints(builtList);

        assertEquals(builtList, deserialized.ints());
        assertEquals(builtList, built.ints());
    }

    @Test
    public void mapOfIntegersToIntegers() {
        MapSchema deserialized = deserialize("{:ints {1 2, 3 4}}", MapSchema.class);
        Map<Integer, Integer> builtMap = new HashMap<>();
        builtMap.put(1, 2);
        builtMap.put(3, 4);
        MapSchema built = newInstance(MapSchema.class).ints(builtMap);

        assertEquals(builtMap, deserialized.ints());
        assertEquals(builtMap, built.ints());
    }

    @Test
    public void largeList() {
        List<String> strings = range(0, 10_000).mapToObj(n -> string()).collect(toList());

        ListSchema listSchema = newInstance(ListSchema.class).strings(strings);

        assertEquals(strings, listSchema.strings());
    }

    @Test
    public void largeMap() {
        Map<String, String> map = range(0, 10_000).boxed().collect(toMap(n -> string(), n -> string()));

        MapSchema mapSchema = newInstance(MapSchema.class).dictionary(map);

        assertEquals(map.size(), mapSchema.dictionary().size());
        assertEquals(map, mapSchema.dictionary());
    }

    private static String string() {
        byte[] buf = new byte[64];
        Random.nextBytes(buf);
        return Encoder.encodeToString(buf);
    }
}

interface ListSchema extends DynamicObject<ListSchema> {
    List<String> strings();
    List<Integer> ints();

    ListSchema strings(List<String> strings);
    ListSchema ints(List<Integer> ints);
}

interface SetSchema extends DynamicObject<SetSchema> {
    Set<String> strings();
}

interface MapSchema extends DynamicObject<MapSchema> {
    Map<String, String> dictionary();
    Map<Integer, Integer> ints();

    MapSchema dictionary(Map<String, String> dictionary);
    MapSchema ints(Map<Integer, Integer> ints);
}
