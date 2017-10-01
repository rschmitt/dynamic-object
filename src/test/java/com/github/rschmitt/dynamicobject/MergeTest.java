package com.github.rschmitt.dynamicobject;

import static com.github.rschmitt.dynamicobject.TestUtils.assertEquivalent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MergeTest {
    @BeforeEach
    public void setup() {
        DynamicObject.registerTag(Mergeable.class, "M");
    }

    @AfterEach
    public void teardown() {
        DynamicObject.deregisterTag(Mergeable.class);
    }

    @Test
    public void twoEmptyObjects() {
        Mergeable a = DynamicObject.deserialize("#M{:a nil}", Mergeable.class);
        Mergeable b = DynamicObject.deserialize("#M{:a nil}", Mergeable.class);

        Mergeable c = a.merge(b);

        assertEquals("#M{:a nil}", DynamicObject.serialize(c));
    }

    @Test
    public void twoFullObjects() {
        Mergeable a = DynamicObject.deserialize("#M{:a \"first\"}", Mergeable.class);
        Mergeable b = DynamicObject.deserialize("#M{:a \"second\"}", Mergeable.class);

        Mergeable c = a.merge(b);

        assertEquals("#M{:a \"second\"}", DynamicObject.serialize(c));
    }

    @Test
    public void nullsDoNotReplaceNonNulls() {
        Mergeable a = DynamicObject.deserialize("#M{:a \"first\"}", Mergeable.class);
        Mergeable b = DynamicObject.deserialize("#M{:a nil}", Mergeable.class);

        Mergeable c = a.merge(b);

        assertEquals("#M{:a \"first\"}", DynamicObject.serialize(c));
    }

    @Test
    public void mergeOutputSerializesCorrectly() {
        Mergeable a = DynamicObject.deserialize("#M{:a \"outer\"}", Mergeable.class);
        Mergeable b = DynamicObject.deserialize("#M{:m #M{:a \"inner\"}}", Mergeable.class);

        Mergeable c = a.merge(b);

        assertEquivalent("#M{:m #M{:a \"inner\"}, :a \"outer\"}", DynamicObject.serialize(c));
    }

    public interface Mergeable extends DynamicObject<Mergeable> {
        String a();
        Mergeable m();
    }
}
