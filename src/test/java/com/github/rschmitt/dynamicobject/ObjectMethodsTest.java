package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class ObjectMethodsTest {
    @Test
    public void equalsNullTest() throws Exception {
        assertFalse(DynamicObject.newInstance(DynamicObject.class).equals(null));
    }
}
