package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.rschmitt.dynamicobject.DynamicObject.serialize;
import static org.junit.Assert.assertEquals;

public class NestingTest {
    @Test
    public void nestedInts() {
        List<Integer> innerList = new ArrayList<>();
        innerList.add(1);
        innerList.add(2);
        innerList.add(3);
        List<List<Integer>> outerList = new ArrayList<>();
        outerList.add(innerList);

        Nested nested = DynamicObject.newInstance(Nested.class).nestedIntegers(outerList);

        assertEquals(outerList, nested.nestedIntegers());
        assertEquals("{:nestedIntegers [[1 2 3]]}", serialize(nested));
    }

    @Test
    public void nestedStrings() {
        List<String> innerList = new ArrayList<>();
        innerList.add("str1");
        innerList.add("str2");
        innerList.add("str3");
        List<List<String>> outerList = new ArrayList<>();
        outerList.add(innerList);

        Nested nested = DynamicObject.newInstance(Nested.class).nestedStrings(outerList);

        assertEquals(outerList, nested.nestedStrings());
    }

    @Test
    public void nestedMaps() {
        Map<Integer, Integer> innerMap = new HashMap<>();
        innerMap.put(1, 2);
        Map<Integer, Map<Integer, Integer>> outerMap = new HashMap<>();
        outerMap.put(1, innerMap);

        Nested nested = DynamicObject.newInstance(Nested.class).nestedMaps(outerMap);

        assertEquals(outerMap, nested.nestedMaps());
        assertEquals("{:nestedMaps {1 {1 2}}}", serialize(nested));
    }

    public interface Nested extends DynamicObject<Nested> {
        List<List<String>> nestedStrings();
        List<List<Integer>> nestedIntegers();
        Map<Integer, Map<Integer, Integer>> nestedMaps();

        Nested nestedStrings(List<List<String>> strings);
        Nested nestedIntegers(List<List<Integer>> integers);
        Nested nestedMaps(Map<Integer, Map<Integer, Integer>> nestedMaps);
    }
}
