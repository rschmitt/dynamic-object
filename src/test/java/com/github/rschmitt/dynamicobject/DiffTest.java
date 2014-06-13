package com.github.rschmitt.dynamicobject;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.serialize;
import static org.junit.Assert.*;

public class DiffTest {
    @Before
    public void setup() {
        DynamicObject.registerTag(Diffable.class, "D");
    }

    @After
    public void teardown() {
        DynamicObject.deregisterTag(Diffable.class);
    }

    @Test
    public void union() {
        Diffable a = deserialize("#D{:a \"a\", :b \"b\"}", Diffable.class);
        Diffable b = deserialize("#D{:a \"a\", :b \"b\", :c \"c\"}", Diffable.class);

        Diffable c = a.union(b);

        assertEquals(c, a);
        assertNotEquals(c, b);
        assertEquals("#D{:a \"a\", :b \"b\"}", serialize(c));
    }

    @Test
    public void emptyUnion() {
        Diffable a = deserialize("#D{:a \"a\"}", Diffable.class);
        Diffable b = deserialize("#D{:b \"b\"}", Diffable.class);

        Diffable c = a.union(b);

        assertNull(c.a());
        assertNull(c.b());
        assertEquals("#D{}", serialize(c));
    }

    @Test
    public void mapSubdiff() {
        Diffable a = deserialize("#D{:d #D{:a \"inner\"},                 :a \"a\", :b \"?\"}", Diffable.class);
        Diffable b = deserialize("#D{:d #D{:a \"inner\", :b \"ignored\"}, :a \"a\", :b \"!\"}", Diffable.class);

        Diffable c = a.union(b);

        assertEquals("a", c.a());
        assertNull(c.b());
        assertEquals(a.d(), c.d());
        assertEquals("inner", a.d().a());
    }

    @Test
    public void setsAreNotSubdiffed() {
        Diffable a = deserialize("#D{:s #{1 2 3}}", Diffable.class);
        Diffable b = deserialize("#D{:s #{1 2 3 4}}", Diffable.class);

        Diffable c = a.union(b);

        assertEquals(null, c.set());
    }

    @Test
    @Ignore("This test is currently failing due to a bug in Reification.wrapElements")
    public void listsAreSubdiffed() {
        Diffable a = deserialize("#D{:list [5 4 3]}", Diffable.class);
        Diffable b = deserialize("#D{:list [1 2 3]}", Diffable.class);

        Diffable c = a.union(b);

        assertEquals(null, c.list().get(0));
        assertEquals(null,               c.list().get(1));
        assertEquals(Integer.valueOf(3), c.list().get(2));
    }
}

interface Diffable extends DynamicObject<Diffable> {
    String a();
    String b();
    Diffable d();
    Set<Integer> set();
    List<Integer> list();
}