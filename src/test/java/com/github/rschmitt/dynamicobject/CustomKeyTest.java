package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CustomKeyTest {
    @Test
    public void customKeywordSupport() {
        String edn = "{:a-sample-int 5}";
        KeywordInterface object = DynamicObject.deserialize(edn, KeywordInterface.class);
        assertEquals(5, object.aSampleInt());
    }

    @Test
    public void colonsAreIgnored() {
        String edn = "{:another-sample-int 7}";
        KeywordInterface object = DynamicObject.deserialize(edn, KeywordInterface.class);
        assertEquals(7, object.anotherSampleInt());
    }
}

interface KeywordInterface extends DynamicObject<KeywordInterface> {
    @Key("a-sample-int")
    int aSampleInt();

    @Key(":another-sample-int")
    int anotherSampleInt();
}
