package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collection;

import static org.junit.Assert.*;

public class ReflectionTest {
    @Test
    public void fieldGetters() throws Exception {
        Collection<Method> methods = Reflection.fieldGetters(Type.class);

        assertEquals(2, methods.size());
        assertTrue(methods.contains(Type.class.getMethod("num")));
        assertTrue(methods.contains(Type.class.getMethod("customKey")));
    }
}

interface Type extends DynamicObject<Type> {
    int num();
    @Key(":custom-key") String customKey();
    @Meta String someMetadata();

    Type num(int num);

    default void customMethod() {

    }

    default boolean anotherCustomMethod(int x) {
        return true;
    }
}
