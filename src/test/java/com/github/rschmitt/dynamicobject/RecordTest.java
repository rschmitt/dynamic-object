package com.github.rschmitt.dynamicobject;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RecordTest {
    @Before
    public void setup() {
        DynamicObject.registerTag(Defrecord.class, "com.github.rschmitt.dynamicobject.Defrecord");
        DynamicObject.registerTag(Random.class, "com.github.rschmitt.dynamicobject.Random");
    }

    @After
    public void teardown() {
        DynamicObject.deregisterTag(Defrecord.class);
        DynamicObject.deregisterTag(Random.class);
    }

    @Test
    public void roundTrip() {
        String edn = "#com.github.rschmitt.dynamicobject.Defrecord{:str \"a string\"}";

        Defrecord record = DynamicObject.deserialize(edn, Defrecord.class);

        assertEquals("a string", record.str());
        assertEquals(edn, DynamicObject.serialize(record));
    }

    @Test
    public void pprintIncludesReaderTag() {
        String edn = "#com.github.rschmitt.dynamicobject.Defrecord{:str \"a string\"}";
        Defrecord record = DynamicObject.deserialize(edn, Defrecord.class);
        assertEquals(format("%s%n", edn), record.toFormattedString());
    }

    @Test(expected = ClassCastException.class)
    public void suppliedTypeMustMatchReaderTag() {
        String edn = "#com.github.rschmitt.dynamicobject.Defrecord{:str \"a string\"}";

        DynamicObject.deserialize(edn, Random.class);
    }

    public interface Defrecord extends DynamicObject<Defrecord> {
        String str();
    }

    public interface Random extends DynamicObject<Random> {
        String str();
    }
}
