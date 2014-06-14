package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;

public class ValidationTest {
    @Test(expected = IllegalStateException.class)
    public void requiredFieldsMissing() {
        try {
            deserialize("{:z 19}", RequiredFields.class).validate();
        } catch (Throwable t) {
            System.out.println(t.toString());
            throw t;
        }
    }

    @Test(expected = IllegalStateException.class)
    public void requiredFieldsNull() {
        deserialize("{:x nil, :y nil}", RequiredFields.class).validate();
    }

    @Test
    public void requiredFieldsPresent() {
        deserialize("{:x 1, :y 2}", RequiredFields.class).validate();
    }

    @Test
    public void allFieldsPresent() {
        deserialize("{:x 1, :y 2, :z 3}", RequiredFields.class).validate();
    }
}

interface RequiredFields extends DynamicObject<RequiredFields> {
    @Required int x();
    @Required int y();
    int z();
}
