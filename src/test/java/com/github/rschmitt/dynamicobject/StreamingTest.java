package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import java.io.PushbackReader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class StreamingTest {
    @Test
    public void iteratorTest() {
        String edn = "{:x 1} {:x 2}";
        PushbackReader reader = new PushbackReader(new StringReader(edn));

        Iterator<StreamingType> iterator = DynamicObject.deserializeStream(reader, StreamingType.class).iterator();

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

        Stream<StreamingType> stream = DynamicObject.deserializeStream(reader, StreamingType.class);

        assertEquals(6, stream.mapToInt(StreamingType::x).sum());
    }
}

interface StreamingType extends DynamicObject<StreamingType> {
    int x();
}
