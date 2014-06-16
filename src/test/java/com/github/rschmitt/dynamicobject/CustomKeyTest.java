package com.github.rschmitt.dynamicobject;

import org.junit.Assert;
import org.junit.Test;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.newInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CustomKeyTest {
    @Test
    public void customKeywordSupport() {
        String edn = "{:a-sample-int 5}";
        KeywordInterface object = deserialize(edn, KeywordInterface.class);
        assertEquals(5, object.aSampleInt());
        assertEquals(object, newInstance(KeywordInterface.class).aSampleInt(5));
    }

    @Test
    public void colonsAreIgnored() {
        String edn = "{:another-sample-int 7}";
        KeywordInterface object = deserialize(edn, KeywordInterface.class);
        assertEquals(7, object.anotherSampleInt());
        assertEquals(object, newInstance(KeywordInterface.class).anotherSampleInt(7));

        assertNull(newInstance(Null.class).boxed());
        try {
            newInstance(Null.class).unboxed();
            Assert.fail();
        } catch (NullPointerException ignore) {}
    }
}

interface KeywordInterface extends DynamicObject<KeywordInterface> {
    @Key("a-sample-int") int aSampleInt();
    @Key(":another-sample-int") int anotherSampleInt();

    KeywordInterface aSampleInt(int aSampleInt);
    KeywordInterface anotherSampleInt(int anotherSampleInt);
}

interface Null extends DynamicObject<Null> {
    int unboxed();
    Integer boxed();
}
