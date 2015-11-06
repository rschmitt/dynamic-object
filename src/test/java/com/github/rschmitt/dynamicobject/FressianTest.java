package com.github.rschmitt.dynamicobject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FressianTest {
    public static final BinarySerialized SAMPLE_VALUE
            = DynamicObject.newInstance(BinarySerialized.class).withHello("world");

    @Before
    public void setup() {
        DynamicObject.registerTag(BinarySerialized.class, "BinarySerialized");
    }

    @After
    public void teardown() {
        DynamicObject.deregisterTag(BinarySerialized.class);
    }

    @Test
    public void smokeTest() throws Exception {
        byte[] bytes = DynamicObject.toFressianByteArray(SAMPLE_VALUE);
        Object o = DynamicObject.fromFressianByteArray(bytes);

        assertEquals(o, SAMPLE_VALUE);
    }

    @Test
    public void formatCompatibilityTest() throws Exception {
        String encoded = "7+MQQmluYXJ5U2VyaWFsaXplZAHA5sr3zd9oZWxsb993b3JsZA==";
        BinarySerialized deserialized = DynamicObject.fromFressianByteArray(Base64.getDecoder().decode(encoded));

        assertEquals(SAMPLE_VALUE, deserialized);
    }

    @Test
    public void cachedKeys_canBeRoundTripped() throws Exception {
        String cachedValue = "cached value";
        BinarySerialized value = DynamicObject.newInstance(BinarySerialized.class).withCached(cachedValue);

        byte[] fressian = DynamicObject.toFressianByteArray(Arrays.asList(value, value));
        List<BinarySerialized> deserialized = DynamicObject.fromFressianByteArray(fressian);

        assertEquals(value, deserialized.get(0));
        assertEquals(value, deserialized.get(1));
    }

    @Test
    public void cachedKeys_areNotRepeated() throws Exception {
        String cachedValue = "cached value";
        BinarySerialized value = DynamicObject.newInstance(BinarySerialized.class).withCached(cachedValue);

        byte[] fressian = DynamicObject.toFressianByteArray(Arrays.asList(value, value));
        // Interpret as an 8-bit charset just to make it easy to find the embedded string(s)
        String s = new String(fressian, "ISO-8859-1");

        int firstIndex = s.indexOf(cachedValue);
        assertTrue(firstIndex >= 0);

        int secondIndex = s.indexOf(cachedValue, firstIndex + 1);
        assertEquals(-1, secondIndex);
    }

    public interface BinarySerialized extends DynamicObject<BinarySerialized> {
        @Key(":hello") BinarySerialized withHello(String hello);
        @Cached @Key(":cached") BinarySerialized withCached(String cached);
    }
}
