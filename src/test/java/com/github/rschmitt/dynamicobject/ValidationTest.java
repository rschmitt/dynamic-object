package com.github.rschmitt.dynamicobject;

import org.junit.*;

import static com.github.rschmitt.dynamicobject.DynamicObject.deregisterTag;
import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.registerTag;

public class ValidationTest {
    @BeforeClass
    public static void setup() {
        registerTag(RequiredFields.class, "R");
        registerTag(Mismatch.class, "M");
        registerTag(Inner.class, "I");
        registerTag(Outer.class, "O");
    }

    @AfterClass
    public static void teardown() {
        deregisterTag(RequiredFields.class);
        deregisterTag(Mismatch.class);
        deregisterTag(Inner.class);
        deregisterTag(Outer.class);
    }

    @Test(expected = IllegalStateException.class)
    public void requiredFieldsMissing() {
        deserialize("#R{:z 19}", RequiredFields.class).validate();
    }

    @Test(expected = IllegalStateException.class)
    public void requiredFieldsNull() {
        deserialize("#R{:x nil, :y nil}", RequiredFields.class).validate();
    }

    @Test
    public void requiredFieldsPresent() {
        deserialize("#R{:x 1, :y 2}", RequiredFields.class).validate();
    }

    @Test
    public void allFieldsPresent() {
        deserialize("#R{:x 1, :y 2, :z 3}", RequiredFields.class).validate();
    }

    @Test(expected = IllegalStateException.class)
    public void requiredFieldHasWrongType() {
        deserialize("#M{:required-string 4}", Mismatch.class).validate();
    }

    @Test(expected = IllegalStateException.class)
    public void optionalFieldHasWrongType() {
        deserialize("#M{:required-string \"str\", :optional-string 4}", Mismatch.class).validate();
    }

    @Test(expected = IllegalStateException.class)
    public void optionalNumericFieldHasWrongType() {
        deserialize("#R{:x \"strings!\", :y \"moar strings!\"}", RequiredFields.class).validate();
    }

    @Test
    public void nestedInstanceIsOk() {
        deserialize("#O{:inner #I{:x 4}}", Outer.class).validate();
    }

    @Test(expected = IllegalStateException.class)
    public void nestedInstanceIsMissingRequiredField() {
        deserialize("#O{:inner #I{}}", Outer.class).validate();
    }

    @Test(expected = IllegalStateException.class)
    public void nestedInstanceContainsTypeMismatch() {
        deserialize("#O{:inner #I{:x \"strings!\"}}", Outer.class).validate();
    }

    @Test(expected = IllegalStateException.class)
    public void nestedInstanceIsWrongType() {
        deserialize("#O{:inner 4}", Outer.class).validate();
    }
}

interface RequiredFields extends DynamicObject<RequiredFields> {
    @Required int x();
    @Required int y();
    int z();
}

interface Mismatch extends DynamicObject<Mismatch> {
    @Key(":required-string") @Required String requiredString();
    @Key(":optional-string") String optionalString();
}

interface Inner extends DynamicObject<Inner> {
    @Required int x();
}

interface Outer extends DynamicObject<Outer> {
    @Required Inner inner();
}