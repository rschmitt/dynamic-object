package com.github.rschmitt.dynamicobject;

import org.junit.*;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import static com.github.rschmitt.dynamicobject.ClojureStuff.READ_STRING;
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
        registerTag(ListContainer.class, "LC");
    }

    @AfterClass
    public static void teardown() {
        deregisterTag(RequiredFields.class);
        deregisterTag(Mismatch.class);
        deregisterTag(Inner.class);
        deregisterTag(Outer.class);
        deregisterTag(ListContainer.class);
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

    @Test(expected = IllegalStateException.class)
    public void listHasWrongElementType() {
        deserialize("#LC{:list [\"string!\" \"another string!\"]}", ListContainer.class).validate();
    }

    @Test
    public void listOfDynamicObjects() throws Exception {
        deserialize("#LC{:inner [#I{:x 1}, #I{:x 2}]}", ListContainer.class).validate();
    }

    @Test(expected = IllegalStateException.class)
    public void listOfDynamicObjectsWithNullFields() throws Exception {
        deserialize("#LC{:inner [#I{:x nil}, #I{:x nil}]}", ListContainer.class).validate();
    }

    @Test(expected = IllegalStateException.class)
    public void listOfDynamicObjectsWithMissingFields() throws Exception {
        deserialize("#LC{:inner [#I{}, #I{}]}", ListContainer.class).validate();
    }

    @Test(expected = IllegalStateException.class)
    public void listOfDynamicObjectsWithTypeErrors() throws Exception {
        deserialize("#LC{:inner [#I{:x 1}, #I{:x \"str\"}]}", ListContainer.class).validate();
    }

    @Test
    public void rawList() throws Exception {
        List<?> list = (List<?>) READ_STRING.invoke("[\"str1\", \"str2\", 3]");
        Type expectedType = CompoundLists.class.getMethod("rawList").getGenericReturnType();
        Validation.validateCollection(list, expectedType);
    }

    @Test
    public void listOfStrings() throws Exception {
        List<?> list = (List<?>) READ_STRING.invoke("[\"str1\", \"str2\", nil, \"str4\"]");
        Type expectedType = CompoundLists.class.getMethod("strings").getGenericReturnType();
        Validation.validateCollection(list, expectedType);
    }

    @Test(expected = IllegalStateException.class)
    public void invalidListOfStrings() throws Exception {
        List<?> list = (List<?>) READ_STRING.invoke("[\"str1\", \"str2\", 3]");
        Type expectedType = CompoundLists.class.getMethod("strings").getGenericReturnType();
        Validation.validateCollection(list, expectedType);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void wildcardList() throws Exception {
        List<?> list = (List<?>) READ_STRING.invoke("[\"str1\", \"str2\", 3]");
        Type expectedType = CompoundLists.class.getMethod("wildcardList").getGenericReturnType();
        Validation.validateCollection(list, expectedType);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void nestedList() throws Exception {
        List<?> list = (List<?>) READ_STRING.invoke("[[\"str1\"], [\"str2\"]]");
        Type expectedType = CompoundLists.class.getMethod("listOfListOfStrings").getGenericReturnType();
        Validation.validateCollection(list, expectedType);
    }

    @Test
    public void rawSet() throws Exception {
        Set<?> set = (Set<?>) READ_STRING.invoke("#{\"str1\", \"str2\", 3}");
        Type expectedType = CompoundSets.class.getMethod("rawSet").getGenericReturnType();
        Validation.validateCollection(set, expectedType);
    }

    @Test
    public void setOfStrings() throws Exception {
        Set<?> set = (Set<?>) READ_STRING.invoke("#{\"str1\", \"str2\", nil, \"str4\"}");
        Type expectedType = CompoundSets.class.getMethod("strings").getGenericReturnType();
        Validation.validateCollection(set, expectedType);
    }

    @Test(expected = IllegalStateException.class)
    public void invalidSetOfStrings() throws Exception {
        Set<?> set = (Set<?>) READ_STRING.invoke("#{\"str1\", \"str2\", 3}");
        Type expectedType = CompoundSets.class.getMethod("strings").getGenericReturnType();
        Validation.validateCollection(set, expectedType);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void wildcardSet() throws Exception {
        Set<?> set = (Set<?>) READ_STRING.invoke("#{\"str1\", \"str2\", 3}");
        Type expectedType = CompoundSets.class.getMethod("wildcardSet").getGenericReturnType();
        Validation.validateCollection(set, expectedType);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void nestedSet() throws Exception {
        Set<?> set = (Set<?>) READ_STRING.invoke("#{#{\"str1\"}, #{\"str2\"}}");
        Type expectedType = CompoundSets.class.getMethod("setOfSetOfStrings").getGenericReturnType();
        Validation.validateCollection(set, expectedType);
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

interface ListContainer extends DynamicObject<ListContainer> {
    List<BigInteger> list();
    List<Inner> inner();
}

interface CompoundLists extends DynamicObject<CompoundLists> {
    List<String> strings();
    List<Integer> ints();
    List rawList();
    List<?> wildcardList();
    List<List<String>> listOfListOfStrings();
}

interface CompoundSets extends DynamicObject<CompoundSets> {
    Set<String> strings();
    Set<Integer> ints();
    Set rawSet();
    Set<?> wildcardSet();
    Set<Set<String>> setOfSetOfStrings();
}