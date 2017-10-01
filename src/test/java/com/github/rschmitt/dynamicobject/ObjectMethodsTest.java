package com.github.rschmitt.dynamicobject;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class ObjectMethodsTest {
    @Test
    public void equalsNullTest() throws Exception {
        assertFalse(DynamicObject.newInstance(DynamicObject.class).equals(null));
    }
}
