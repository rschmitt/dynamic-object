package com.github.rschmitt.dynamicobject;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RecordTest {
    @BeforeEach
    public void setup() {
        DynamicObject.registerTag(Defrecord.class, "com.github.rschmitt.dynamicobject.Defrecord");
        DynamicObject.registerTag(Random.class, "com.github.rschmitt.dynamicobject.Random");
    }

    @AfterEach
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

    @Test
    public void suppliedTypeMustMatchReaderTag() {
        String edn = "#com.github.rschmitt.dynamicobject.Defrecord{:str \"a string\"}";

        assertThrows(ClassCastException.class, () -> DynamicObject.deserialize(edn, Random.class));
    }

    public interface Defrecord extends DynamicObject<Defrecord> {
        String str();
    }

    public interface Random extends DynamicObject<Random> {
        String str();
    }
}
