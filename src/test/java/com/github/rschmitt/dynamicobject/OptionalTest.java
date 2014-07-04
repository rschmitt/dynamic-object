package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.newInstance;
import static com.github.rschmitt.dynamicobject.DynamicObject.serialize;
import static java.util.Arrays.asList;
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

    @Test
    public void listPresent() {
        OptWrapper instance = deserialize("{:ints [1 2 3]}", OptWrapper.class);
        OptWrapper expected = newInstance(OptWrapper.class).ints(Optional.of(asList(1, 2, 3)));

        assertEquals(asList(1, 2, 3), instance.ints().get());
        assertEquals(expected, instance);
    }

    @Test
    public void instantPresent() {
        String edn = "{:inst #inst \"1985-04-12T23:20:50.520-00:00\"}";
        Instant expected = Instant.parse("1985-04-12T23:20:50.52Z");

        OptWrapper instance = deserialize(edn, OptWrapper.class);

        assertEquals(expected, instance.inst().get());
        assertEquals(edn, serialize(instance));
    }

    @Test
    public void dynamicObjectPresent() {
        DynamicObject.registerTag(OptWrapper.class, "OptWrapper");

        OptWrapper instance = deserialize("#OptWrapper{:wrapper #OptWrapper{:i 24}}", OptWrapper.class);
        OptWrapper expected = newInstance(OptWrapper.class).wrapper(Optional.of(newInstance(OptWrapper.class).i(Optional.of(24))));

        assertEquals(expected.wrapper().get(), instance.wrapper().get());
        assertEquals(expected, instance);

        DynamicObject.deregisterTag(OptWrapper.class);
    }
}

interface OptWrapper extends DynamicObject<OptWrapper> {
    Optional<String> str();
    Optional<Integer> i();
    Optional<List<Integer>> ints();
    Optional<Instant> inst();
    Optional<OptWrapper> wrapper();

    OptWrapper str(Optional<String> str);
    OptWrapper i(Optional<Integer> i);
    OptWrapper ints(Optional<List<Integer>> ints);
    OptWrapper inst(Optional<Instant> inst);
    OptWrapper wrapper(Optional<OptWrapper> wrapper);
}