package com.github.rschmitt.dynamicobject;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FressianTest {
    public static final BinarySerialized SAMPLE_VALUE
            = DynamicObject.newInstance(BinarySerialized.class).withHello("world");

    @BeforeEach
    public void setup() {
        DynamicObject.registerTag(BinarySerialized.class, "BinarySerialized");
    }

    @AfterEach
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
    public void testNullValues() throws Exception {
        BinarySerialized testValue = SAMPLE_VALUE.withNull(null);

        byte[] bytes = DynamicObject.toFressianByteArray(testValue);
        Object o = DynamicObject.fromFressianByteArray(bytes);

        assertEquals(o, testValue);
    }

    @Test
    public void formatCompatibilityTest() throws Exception {
        String encoded = "7+MQQmluYXJ5U2VyaWFsaXplZAHA5sr3zd9oZWxsb993b3JsZA==";
        BinarySerialized deserialized = DynamicObject.fromFressianByteArray(Base64.getDecoder().decode(encoded));

        assertEquals(SAMPLE_VALUE, deserialized);
    }

    @Test
    public void repeatedSerializationYieldsSameResultWith8Fields() throws Exception {
        // This only surfaces with 8 fields, because Clojure Fressian deserializes maps with 8 keys
        // as a PersistentHashMap, when it should deserialize it as a PersistentArrayMap.

        DynamicObject.registerTag(DOWith8Fields.class, "doWith8Fields");
        DOWith8Fields initialDO = DynamicObject.newInstance(DOWith8Fields.class)
                .withField1("a")
                .withField2("b")
                .withField3("c")
                .withField4("d")
                .withField5("e")
                .withField6("f")
                .withField7("g")
                .withField8("h");

        byte[] serialized = DynamicObject.toFressianByteArray(initialDO);
        DOWith8Fields deserialized = DynamicObject.fromFressianByteArray(serialized);

        byte[] reserialized = DynamicObject.toFressianByteArray(deserialized);

        assertArrayEquals(serialized, reserialized);
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
    public void deregisteringAClassRepeatedly_doesNotThrowAnNPE() throws Exception {
        // First call to deregister & remove values from internal caches
        DynamicObject.deregisterTag(BinarySerialized.class);

        // This call should not throw an exception
        DynamicObject.deregisterTag(BinarySerialized.class);
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
        @Key(":null") BinarySerialized withNull(Object nil);
        @Cached @Key(":cached") BinarySerialized withCached(String cached);
    }

    public interface DOWith8Fields extends DynamicObject<DOWith8Fields> {
        @Key(":field1") DOWith8Fields withField1(String field1);
        @Key(":field2") DOWith8Fields withField2(String field2);
        @Key(":field3") DOWith8Fields withField3(String field3);
        @Key(":field4") DOWith8Fields withField4(String field4);
        @Key(":field5") DOWith8Fields withField5(String field5);
        @Key(":field6") DOWith8Fields withField6(String field6);
        @Key(":field7") DOWith8Fields withField7(String field7);
        @Key(":field8") DOWith8Fields withField8(String field8);
    }
}
