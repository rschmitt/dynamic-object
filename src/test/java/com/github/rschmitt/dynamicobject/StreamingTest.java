package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import org.junit.Test;

import java.io.PushbackReader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserializeStream;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class StreamingTest {
    @Test
    public void iteratorTest() {
        String edn = "{:x 1} {:x 2}";
        PushbackReader reader = new PushbackReader(new StringReader(edn));

        Iterator<StreamingType> iterator = deserializeStream(reader, StreamingType.class).iterator();

        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertEquals(1, iterator.next().x());
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertEquals(2, iterator.next().x());
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void streamTest() {
        String edn = "{:x 1}{:x 2}{:x 3}";
        PushbackReader reader = new PushbackReader(new StringReader(edn));

        Stream<StreamingType> stream = deserializeStream(reader, StreamingType.class);

        assertEquals(6, stream.mapToInt(StreamingType::x).sum());
    }

    @Test
    public void plainStreamTest() {
        String edn = "\"string one\"\n\"string two\"";
        PushbackReader reader = new PushbackReader(new StringReader(edn));

        List<String> list = deserializeStream(reader, String.class).collect(toList());

        assertEquals(asList("string one", "string two"), list);
    }

    @Test
    public void heterogeneousStreamTest() {
        List expected = asList(42L, "string", Clojure.read("{:key :value}"), asList('a', 'b', 'c'));
        String edn = "42 \"string\" {:key :value} [\\a \\b \\c]";
        PushbackReader reader = new PushbackReader(new StringReader(edn));

        List actual = deserializeStream(reader, Object.class).collect(toList());

        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void nilTest() {
        deserializeStream(new PushbackReader(new StringReader("nil")), StreamingType.class).collect(toList());
    }

    public interface StreamingType extends DynamicObject<StreamingType> {
        int x();
    }
}
