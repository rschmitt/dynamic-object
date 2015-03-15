package com.github.rschmitt.dynamicobject;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FressianTest {
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
        BinarySerialized binarySerialized = DynamicObject.newInstance(BinarySerialized.class).withHello("world");

        byte[] bytes = DynamicObject.toFressianByteArray(binarySerialized);
        Object o = DynamicObject.fromFressianByteArray(bytes);

        assertEquals(o, binarySerialized);
    }

    public interface BinarySerialized extends DynamicObject<BinarySerialized> {
        @Key(":hello") BinarySerialized withHello(String hello);
    }
}
