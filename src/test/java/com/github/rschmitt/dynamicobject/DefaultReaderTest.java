package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultReaderTest {
    @Before
    public void setup() {
        DynamicObject.setDefaultReader(Unknown::new);
    }

    @After
    public void teardown() {
        DynamicObject.setDefaultReader(null);
    }

    @Test
    public void testUnknownReader() {
        Object obj = DynamicObject.deserialize("#some-namespace/some-record-name{:key :value}", Object.class);
        Unknown unknown = (Unknown) obj;

        assertEquals("some-namespace/some-record-name", unknown.getTag());
        assertEquals(Clojure.read("{:key :value}"), unknown.getObj());
    }
}

class Unknown {
    private final String tag;
    private final Object obj;

    Unknown(String tag, Object obj) {
        this.tag = tag;
        this.obj = obj;
    }

    public String getTag() {
        return tag;
    }

    public Object getObj() {
        return obj;
    }
}