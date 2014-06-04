package com.github.rschmitt.dynamicobject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.newInstance;
import static com.github.rschmitt.dynamicobject.DynamicObject.serialize;
import static org.junit.Assert.assertEquals;

public class SchemaCollectionTest {
    @Before
    public void setup() {
        DynamicObject.registerTag(X.class, "X");
        DynamicObject.registerTag(Coll.class, "Coll");
    }

    @After
    public void teardown() {
        DynamicObject.deregisterTag(Coll.class);
        DynamicObject.deregisterTag(X.class);
    }

    @Test
    public void list() {
        Coll expected = deserialize("#Coll{:list [#X{:y 1}, #X{:y 2}, #X{:y 3}]}", Coll.class);
        List<X> list = new ArrayList<>();
        list.add(newInstance(X.class).y(1));
        list.add(newInstance(X.class).y(2));
        list.add(newInstance(X.class).y(3));

        Coll actual = newInstance(Coll.class).list(list);

        roundTripTest(expected, Coll.class);
        assertEquals(expected, actual);
        assertEquals(list, expected.list());
        assertEquals(list, actual.list());
        assertEquals(actual.list(), list);
        assertEquals(expected.list(), list);
    }

    @Test
    public void set() {
        Coll expected = deserialize("#Coll{:set #{#X{:y 1} #X{:y 2} #X{:y 3}}}", Coll.class);
        Set<X> set = new HashSet<>();
        set.add(newInstance(X.class).y(1));
        set.add(newInstance(X.class).y(3));
        set.add(newInstance(X.class).y(2));

        Coll actual = newInstance(Coll.class).set(set);

        roundTripTest(expected, Coll.class);
        assertEquals(expected, actual);
        assertEquals(set, expected.set());
        assertEquals(set, actual.set());
        assertEquals(actual.set(), set);
        assertEquals(expected.set(), set);
    }

    private <T extends DynamicObject<T>> void roundTripTest(T obj, Class<T> type) {
        String edn = serialize(obj);
        T actual = deserialize(edn, type);
        assertEquals(obj, actual);
    }
}

interface Coll extends DynamicObject<Coll> {
    List<X> list();
    Set<X> set();
    Map<X, X> map();

    Coll list(List<X> list);
    Coll set(Set<X> set);
    Coll map(Map<X, X> map);
}

interface X extends DynamicObject<X> {
    int y();

    X y(int y);
}