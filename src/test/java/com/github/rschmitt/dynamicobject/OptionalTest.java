package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import java.util.Optional;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.newInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class OptionalTest {
    @Test
    public void valuePresent() {
        OptWrapper instance = deserialize("{:str \"value\"}", OptWrapper.class);
        OptWrapper expected = newInstance(OptWrapper.class).str(Optional.of("value"));

        assertEquals("value", instance.str().get());
        assertEquals(expected, instance);
    }

    @Test
    public void valueMissing() {
        OptWrapper instance = deserialize("{:str nil}", OptWrapper.class);
        OptWrapper expected = newInstance(OptWrapper.class).str(Optional.empty());

        assertFalse(instance.str().isPresent());
        assertEquals(expected, instance);
    }

    @Test
    public void intPresent() {
        OptWrapper instance = deserialize("{:i 24601}", OptWrapper.class);
        OptWrapper expected = newInstance(OptWrapper.class).i(Optional.of(24601));

        assertEquals(Integer.valueOf(24601), instance.i().get());
        assertEquals(expected, instance);
    }
}

interface OptWrapper extends DynamicObject<OptWrapper> {
    Optional<String> str();
    Optional<Integer> i();

    OptWrapper str(Optional<String> str);
    OptWrapper i(Optional<Integer> i);
}