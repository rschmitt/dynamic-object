package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import java.io.PushbackReader;
import java.io.StringReader;
import java.util.Iterator;

import static org.junit.Assert.*;

public class StreamingTest {
    @Test
    public void pushbackReader() {
        String edn = "{:x 1} {:x 2}";
        PushbackReader reader = new PushbackReader(new StringReader(edn));

        Iterator<StreamingType> iterator = DynamicObject.deserializeStream(reader, StreamingType.class);

        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertEquals(1, iterator.next().x());
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertEquals(2, iterator.next().x());
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
    }
}

interface StreamingType extends DynamicObject<StreamingType> {
    int x();
}
