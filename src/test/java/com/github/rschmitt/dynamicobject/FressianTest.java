package com.github.rschmitt.dynamicobject;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Base64;

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
        BinarySerialized binarySerialized = SAMPLE_VALUE;

        byte[] bytes = DynamicObject.toFressianByteArray(binarySerialized);
        Object o = DynamicObject.fromFressianByteArray(bytes);

        assertEquals(o, binarySerialized);
    }

    @Test
    public void formatCompatibilityTest() throws Exception {
        String encoded = "7+MQQmluYXJ5U2VyaWFsaXplZAHA5sr3zd9oZWxsb993b3JsZA==";
        BinarySerialized deserialized = DynamicObject.fromFressianByteArray(Base64.getDecoder().decode(encoded));

        assertEquals(SAMPLE_VALUE, deserialized);
    }

    public interface BinarySerialized extends DynamicObject<BinarySerialized> {
        @Key(":hello") BinarySerialized withHello(String hello);
    }
}
