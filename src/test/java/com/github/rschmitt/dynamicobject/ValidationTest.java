package com.github.rschmitt.dynamicobject;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.rschmitt.dynamicobject.ClojureStuff.READ_STRING;
import static com.github.rschmitt.dynamicobject.DynamicObject.*;

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

    @Test
    public void validationSuccessful() {
        validationSuccess("#R{:x 1, :y 2}", RequiredFields.class);
        validationSuccess("#R{:x 1, :y 2, :z 3}", RequiredFields.class);
        validationSuccess("#O{:inner #I{:x 4}}", Outer.class);
        validationSuccess("#LC{:inner [#I{:x 1} #I{:x 2}]}", ListContainer.class);
    }

    @Test
    public void requiredFieldsMissing() {
        validationFailure("#R{}", RequiredFields.class);
        validationFailure("#R{:z 19}", RequiredFields.class);
        validationFailure("#R{:x nil, :y 1}", RequiredFields.class);
        validationFailure("#R{:x 1, :y nil}", RequiredFields.class);
        validationFailure("#R{:x nil, :y nil}", RequiredFields.class);

        validationFailure("#O{:inner #I{}}", Outer.class);
        validationFailure("#LC{:inner [#I{:x nil}, #I{:x nil}]}", ListContainer.class);
        validationFailure("#LC{:inner [#I{}, #I{}]}", ListContainer.class);
    }

    @Test
    public void typeMismatches() {
        validationFailure("#M{:required-string \"str\", :optional-string 4}", Mismatch.class);
        validationFailure("#R{:x \"strings!\", :y \"moar strings!\"}", RequiredFields.class);
        validationFailure("#M{:required-string 4}", Mismatch.class);
        validationFailure("#O{:inner #I{:x \"strings!\"}}", Outer.class);
        validationFailure("#O{:inner 4}", Outer.class);
        validationFailure("#LC{:list [\"string!\" \"another string!\"]}", ListContainer.class);
        validationFailure("#LC{:list #{\"string!\" \"another string!\"}}", ListContainer.class);
        validationFailure("#LC{:inner [#I{:x 1}, #I{:x \"str\"}]}", ListContainer.class);
    }

    @Test
    public void nestedCollections() throws Exception {
        validationSuccess("{:rawList [\"str1\" \"str2\" 3]}", CompoundLists.class);
        validationSuccess("{:strings [\"str1\" \"str2\" nil \"str4\"]}", CompoundLists.class);

        validationSuccess("{:listOfListOfStrings [[\"str1\"] [\"str2\"]]}", CompoundLists.class);
        validationFailure("{:listOfListOfStrings #{[\"str1\"] [\"str2\"]}}", CompoundLists.class);
        validationFailure("{:listOfListOfStrings [#{\"str1\"} #{\"str2\"}]}", CompoundLists.class);
        validationFailure("{:listOfListOfStrings [[\"str1\"] [\"str2\"] 3]}", CompoundLists.class);
        validationFailure("{:listOfListOfStrings [[\"str1\"] [\"str2\"] [3]]}", CompoundLists.class);

        validationSuccess("{:ints #{1 4 3 2}}", CompoundSets.class);
        validationSuccess("{:rawSet #{\"str1\" \"str2\" 3}}", CompoundSets.class);
        validationSuccess("{:strings #{nil \"str1\" \"str2\" \"str4\"}}", CompoundSets.class);
        validationSuccess("{:setOfSetOfStrings nil}", CompoundSets.class);
        validationSuccess("{:setOfSetOfStrings #{nil}}", CompoundSets.class);
        validationSuccess("{:setOfSetOfStrings #{#{}}}", CompoundSets.class);
        validationSuccess("{:setOfSetOfStrings #{#{}}}", CompoundSets.class);
        validationSuccess("{:setOfSetOfStrings #{#{\"str2\"} #{\"str1\"}}}", CompoundSets.class);
        validationSuccess("{:setOfSetOfStrings #{#{\"str2\"} #{\"str1\"}}}", CompoundSets.class);
        validationFailure("{:setOfSetOfStrings #{[\"str1\"] [\"str2\"]}}", CompoundSets.class);
        validationFailure("{:setOfSetOfStrings #{#{\"str1\"} #{\"str2\"} 3}}", CompoundSets.class);
        validationFailure("{:setOfSetOfStrings #{#{\"str1\"} #{\"str2\"} #{3}}}", CompoundSets.class);

        validationFailure("{:strings {\"k\" \\newline}}", CompoundMaps.class);
        validationFailure("{:strings {1 nil}}", CompoundMaps.class);
        validationFailure("{:strings {1 {\"k\" \"v\"}}}", CompoundMaps.class);
        validationFailure("{:strings {\"str1\" \"str2\", \"str3\" 4}}", CompoundMaps.class);
        validationSuccess("{:strings nil}", CompoundMaps.class);
        validationSuccess("{:strings {}}", CompoundMaps.class);
        validationSuccess("{:strings {\"a\" \"b\", \"c\" \"d\", \"e\" nil}}", CompoundMaps.class);

        validationSuccess("{:rawMap nil}", CompoundMaps.class);
        validationSuccess("{:rawMap {}}", CompoundMaps.class);
        validationSuccess("{:rawMap {\\newline nil}}", CompoundMaps.class);
        validationSuccess("{:rawMap {\"str1\" \"str2\", 3 4}}", CompoundMaps.class);
        validationSuccess("{:nestedGenericMaps {\"str1\" {\"str2\" \"str3\"}}}", CompoundMaps.class);
        validationSuccess("{:nestedGenericMaps {\"str1\" {\"str2\" nil}}}", CompoundMaps.class);
        validationSuccess("{:nestedGenericMaps {\"str1\" {nil \"str3\"}}}", CompoundMaps.class);
        validationSuccess("{:nestedGenericMaps {\"str1\" {}}}", CompoundMaps.class);

        validationSuccess("{:nestedGenericMaps nil}", CompoundMaps.class);
        validationFailure("{:nestedGenericMaps {1 nil}}", CompoundMaps.class);
        validationFailure("{:nestedGenericMaps [\"not\" \"a\" \"map\"]}", CompoundMaps.class);
        validationFailure("{:nestedGenericMaps {\"key\" [\"not\" \"a\" \"map\"]}}", CompoundMaps.class);
        validationFailure("{:nestedGenericMaps {\"key\" {\"k\" 4}}}", CompoundMaps.class);
        validationFailure("{:nestedGenericMaps {\"key\" {4 \"v\"}}}", CompoundMaps.class);
        validationFailure("{:nestedGenericMaps {1 {\"k\" \"v\"}}}", CompoundMaps.class);
        validationFailure("{:nestedGenericMaps {\"key\" {\"k\" #{\"v\"}}}}", CompoundMaps.class);

        validationSuccess("{:nestedNumericMaps {1 nil}}", CompoundMaps.class);
        validationSuccess("{:nestedNumericMaps {1 {}}}", CompoundMaps.class);
        validationSuccess("{:nestedNumericMaps {1 {2 nil}}}", CompoundMaps.class);
        validationSuccess("{:nestedNumericMaps {1 {2 3.3}}}", CompoundMaps.class);
        validationFailure("{:nestedNumericMaps [1 2 3]}", CompoundMaps.class);
        validationFailure("{:nestedNumericMaps {1 [2 3 4]}}", CompoundMaps.class);
        validationFailure("{:nestedNumericMaps {\"k\" {2 3}}}", CompoundMaps.class);
        validationFailure("{:nestedNumericMaps {1 {\"k\" 3}}}", CompoundMaps.class);
        validationFailure("{:nestedNumericMaps {1 {2 \"v\"}}}", CompoundMaps.class);
        validationFailure("{:nestedNumericMaps {1 {\"k\" \"v\"}}}", CompoundMaps.class);
        validationFailure("{:nestedNumericMaps {1 {2 #{3}}}}", CompoundMaps.class);
        validationFailure("{:nestedNumericMaps {1 {#{2} 3}}}", CompoundMaps.class);
        validationFailure("{:nestedNumericMaps {#{1} {2 3}}}", CompoundMaps.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void wildcardList() throws Exception {
        List<?> list = (List<?>) READ_STRING.invoke("[\"str1\", \"str2\", 3]");
        Type expectedType = CompoundLists.class.getMethod("wildcardList").getGenericReturnType();
        Validation.validateCollection(list, expectedType);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void wildcardSet() throws Exception {
        Set<?> set = (Set<?>) READ_STRING.invoke("#{\"str1\", \"str2\", 3}");
        Type expectedType = CompoundSets.class.getMethod("wildcardSet").getGenericReturnType();
        Validation.validateCollection(set, expectedType);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void wildcardKey() throws Exception {
        Map<?, ?> map = (Map<?, ?>) READ_STRING.invoke("{\"str1\" \"str2\"}");
        Type expectedType = CompoundMaps.class.getMethod("wildcardKey").getGenericReturnType();
        Validation.validateMap(map, expectedType);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void wildcardValue() throws Exception {
        deserialize("{:wildcardValue {\"str1\" \"str2\"}}", CompoundMaps.class).validate();
    }

    private static <T extends DynamicObject<T>> void validationFailure(String edn, Class<T> type) {
        try {
            deserialize(edn, type).validate();
            Assert.fail("Expected IllegalStateException");
        } catch (IllegalStateException ex) {
            System.out.println(String.format("%s => %s", edn, ex.getMessage()));
        }
    }

    private static <T extends DynamicObject<T>> void validationSuccess(String edn, Class<T> type) {
        T instance = deserialize(edn, type);
        instance.validate();
        Assert.assertEquals(edn, serialize(instance));
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

interface CompoundMaps extends DynamicObject<CompoundMaps> {
    Map<String, String> strings();
    Map rawMap();
    Map<?, String> wildcardKey();
    Map<String, ?> wildcardValue();
    Map<String, Map<String, String>> nestedGenericMaps();
    Map<Integer, Map<Integer, Float>> nestedNumericMaps();
}