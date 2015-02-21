package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.newInstance;
import static org.junit.Assert.assertEquals;

public class CustomKeyTest {
    @Test
    public void customKeywordSupport() {
        String edn = "{:a-sample-int 5}";
        KeywordInterface object = deserialize(edn, KeywordInterface.class);
        assertEquals(5, object.aSampleInt());
        assertEquals(object, newInstance(KeywordInterface.class).aSampleInt(5));
    }

    @Test
    public void customStringSupport() {
        String edn = "{\"a-sample-string\", \"a-sample-value\"}";
        StringInterface object = deserialize(edn, StringInterface.class);
        assertEquals("a-sample-value", object.sampleString());
        assertEquals(object, newInstance(StringInterface.class).sampleString("a-sample-value"));
    }
}

interface KeywordInterface extends DynamicObject<KeywordInterface> {
    @Key(":a-sample-int") int aSampleInt();

    KeywordInterface aSampleInt(int aSampleInt);
}

interface StringInterface extends DynamicObject<StringInterface> {
    @Key("a-sample-string") String sampleString();

    StringInterface sampleString(String sampleString);
}