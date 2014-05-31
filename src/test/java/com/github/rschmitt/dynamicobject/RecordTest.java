package com.github.rschmitt.dynamicobject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

public class RecordTest {
    @Before
    public void setup() {
        DynamicObject.registerTag(Defrecord.class, "com.github.rschmitt.dynamicobject.Defrecord");
    }

    @After
    public void teardown() {
        DynamicObject.deregisterTag(Defrecord.class, "com.github.rschmitt.dynamicobject.Defrecord");
    }

    @Test
    public void roundTrip() {
        String edn = "#com.github.rschmitt.dynamicobject.Defrecord{:str \"a string\"}";

        Defrecord record = DynamicObject.deserialize(edn, Defrecord.class);

        assertEquals("a string", record.str());
        assertEquals(edn, DynamicObject.serialize(record));
    }

    @Test
    public void pprintOmitsReaderTag() {
        String edn = "#com.github.rschmitt.dynamicobject.Defrecord{:str \"a string\"}";
        Defrecord record = DynamicObject.deserialize(edn, Defrecord.class);
        assertEquals(format("{:str \"a string\"}%n"), record.toFormattedString());
    }
}

interface Defrecord extends DynamicObject<Defrecord> {
    String str();
}