package com.github.rschmitt.dynamicobject;

import org.junit.After;
import org.junit.Before;
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

    @Before
    public void setup() {
        DynamicObject.registerTag(ListSchema.class, "ls");
        DynamicObject.registerTag(MapSchema.class, "ms");
        DynamicObject.registerTag(SetSchema.class, "ss");
    }

    @After
    public void teardown() {
        DynamicObject.deregisterTag(ListSchema.class);
        DynamicObject.deregisterTag(MapSchema.class);
        DynamicObject.deregisterTag(SetSchema.class);
    }

    @Test
    public void listOfStrings() {
        ListSchema listSchema = deserialize("{:strings [\"one\" \"two\" \"three\"]}", ListSchema.class);
        List<String> stringList = listSchema.strings();
        assertEquals("one", stringList.get(0));
        assertEquals("two", stringList.get(1));
        assertEquals("three", stringList.get(2));
        binaryRoundTrip(listSchema);
    }

    // This is just here to prove a point about Java<->Clojure interop.
    @Test
    public void listStream() {
        ListSchema listSchema = deserialize("{:strings [\"one\" \"two\" \"three\"]}", ListSchema.class);
        List<String> stringList = listSchema.strings();

        List<Integer> collect = stringList.stream().map(String::length).collect(toList());

        assertEquals(3, collect.get(0).intValue());
        assertEquals(3, collect.get(1).intValue());
        assertEquals(5, collect.get(2).intValue());
        binaryRoundTrip(listSchema);
    }

    @Test
    public void setOfStrings() {
        SetSchema setSchema = deserialize("{:strings #{\"one\" \"two\" \"three\"}}", SetSchema.class);
        Set<String> stringSet = setSchema.strings();
        assertEquals(3, stringSet.size());
        assertTrue(stringSet.contains("one"));
        assertTrue(stringSet.contains("two"));
        assertTrue(stringSet.contains("three"));
        binaryRoundTrip(setSchema);
    }

    @Test
    public void embeddedMap() {
        String edn = "{:dictionary {\"key\" \"value\"}}";
        MapSchema mapSchema = deserialize(edn, MapSchema.class);
        assertEquals("value", mapSchema.dictionary().get("key"));
        assertEquals(1, mapSchema.dictionary().size());
        binaryRoundTrip(mapSchema);
    }

    @Test
    public void listOfLongs() {
        ListSchema deserialized = deserialize("{:longs [nil 2 nil 4 nil]}", ListSchema.class);
        List<Long> builtList = asList(null, 2L, null, 4L, null);

        ListSchema built = newInstance(ListSchema.class).longs(builtList);

        assertEquals(builtList, deserialized.longs());
        assertEquals(builtList, built.longs());
        binaryRoundTrip(built);
        binaryRoundTrip(deserialized);
    }

    @Test
    public void mapOfLongsToLongs() {
        MapSchema deserialized = deserialize("{:longs {1 2, 3 4}}", MapSchema.class);
        Map<Long, Long> builtMap = new HashMap<>();
        builtMap.put(1L, 2L);
        builtMap.put(3L, 4L);
        MapSchema built = newInstance(MapSchema.class).longs(builtMap);

        assertEquals(builtMap, deserialized.longs());
        assertEquals(builtMap, built.longs());
        binaryRoundTrip(deserialized);
        binaryRoundTrip(built);
    }

    @Test
    public void largeList() {
        List<String> strings = range(0, 10_000).mapToObj(n -> string()).collect(toList());

        ListSchema listSchema = newInstance(ListSchema.class).strings(strings);

        assertEquals(strings, listSchema.strings());
        binaryRoundTrip(listSchema);
    }

    @Test
    public void largeMap() {
        Map<String, String> map = range(0, 10_000).boxed().collect(toMap(n -> string(), n -> string()));

        MapSchema mapSchema = newInstance(MapSchema.class).dictionary(map);

        assertEquals(map.size(), mapSchema.dictionary().size());
        assertEquals(map, mapSchema.dictionary());
        binaryRoundTrip(mapSchema);
    }

    private void binaryRoundTrip(Object expected) {
        Object actual = DynamicObject.fromFressianByteArray(DynamicObject.toFressianByteArray(expected));
        assertEquals(expected, actual);
    }

    private static String string() {
        byte[] buf = new byte[64];
        Random.nextBytes(buf);
        return Encoder.encodeToString(buf);
    }

    public interface ListSchema extends DynamicObject<ListSchema> {
        List<String> strings();
        List<Long> longs();

        ListSchema strings(List<String> strings);
        ListSchema longs(List<Long> ints);
    }

    public interface SetSchema extends DynamicObject<SetSchema> {
        Set<String> strings();
    }

    public interface MapSchema extends DynamicObject<MapSchema> {
        Map<String, String> dictionary();
        Map<Long, Long> longs();

        MapSchema dictionary(Map<String, String> dictionary);
        MapSchema longs(Map<Long, Long> ints);
    }
}
