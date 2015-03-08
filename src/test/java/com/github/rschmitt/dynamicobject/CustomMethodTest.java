package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CustomMethodTest {
    @Test
    public void invokeCustomMethod() {
        CustomMethod obj = DynamicObject.newInstance(CustomMethod.class);
        assertEquals("asdf", obj.customMethod());
    }

    @Test
    public void invokeGettersFromCustomMethod() {
        CustomMethod obj = DynamicObject.newInstance(CustomMethod.class).str("a string");
        assertEquals("a string", obj.callIntoGetter());
    }

    @Test
    public void invokeCustomWither() {
        CustomMethod obj = DynamicObject.newInstance(CustomMethod.class).customWither(4);
        assertEquals("4", obj.callIntoGetter());
    }

    public interface CustomMethod extends DynamicObject<CustomMethod> {
        String str();

        CustomMethod str(String str);

        default String customMethod() {
            return "asdf";
        }

        default String callIntoGetter() {
            return str();
        }

        default CustomMethod customWither(int x) {
            return str(String.valueOf(x));
        }
    }
}
