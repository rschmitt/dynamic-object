package com.github.rschmitt.dynamicobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SuppressWarnings("unchecked")
public class ObjectMethodsTest {
    @Test
    public void equalsNullTest() throws Exception {
        assertFalse(DynamicObject.newInstance(DynamicObject.class).equals(null));
    }
}
