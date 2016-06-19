package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.newInstance;
import static org.junit.Assert.assertEquals;

public class CustomKeyTest {
    @Test
    public void customKeywordSupport() {
        String edn = "{:a-sample-long 5}";
        KeywordInterface object = deserialize(edn, KeywordInterface.class);
        assertEquals(5L, object.aSampleLong());
        assertEquals(object, newInstance(KeywordInterface.class).aSampleLong(5L));
    }

    @Test
    public void customStringSupport() {
        String edn = "{\"a-sample-string\", \"a-sample-value\"}";
        StringInterface object = deserialize(edn, StringInterface.class);
        assertEquals("a-sample-value", object.sampleString());
        assertEquals(object, newInstance(StringInterface.class).sampleString("a-sample-value"));
    }

    @Test
    public void customBuilderSupport() {
        String edn = "{:element \"a string\"}";

        CustomBuilder expected = deserialize(edn, CustomBuilder.class);
        CustomBuilder actual = newInstance(CustomBuilder.class).withElement("a string");

        assertEquals(expected, actual);
    }

    public interface KeywordInterface extends DynamicObject<KeywordInterface> {
        @Key(":a-sample-long") long aSampleLong();

        KeywordInterface aSampleLong(long aSampleLong);
    }

    public interface StringInterface extends DynamicObject<StringInterface> {
        @Key("a-sample-string") String sampleString();

        StringInterface sampleString(String sampleString);
    }

    public interface CustomBuilder extends DynamicObject<CustomBuilder> {
        @Key(":element") String getElement();
        @Key(":element") CustomBuilder withElement(String element);
    }
}
