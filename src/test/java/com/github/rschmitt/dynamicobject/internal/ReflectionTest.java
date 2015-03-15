package com.github.rschmitt.dynamicobject.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Collection;

import org.junit.Test;

import com.github.rschmitt.dynamicobject.DynamicObject;
import com.github.rschmitt.dynamicobject.Key;
import com.github.rschmitt.dynamicobject.Meta;
import com.github.rschmitt.dynamicobject.Required;

public class ReflectionTest {
    @Test
    public void fieldGetters() throws Exception {
        Collection<Method> methods = Reflection.fieldGetters(VariousMethods.class);

        assertEquals(2, methods.size());
        assertTrue(methods.contains(VariousMethods.class.getMethod("num")));
        assertTrue(methods.contains(VariousMethods.class.getMethod("customKey")));
    }

    @Test
    public void requiredFields() throws Exception {
        Collection<Method> methods = Reflection.requiredFields(VariousMethods.class);

        assertEquals(1, methods.size());
        assertTrue(methods.contains(VariousMethods.class.getMethod("num")));
    }
}

interface VariousMethods extends DynamicObject<VariousMethods> {
    @Required int num();
    @Key(":custom-key") String customKey();
    @Meta String someMetadata();

    VariousMethods num(int num);

    default void customMethod() {

    }

    default boolean anotherCustomMethod(int x) {
        return true;
    }
}
