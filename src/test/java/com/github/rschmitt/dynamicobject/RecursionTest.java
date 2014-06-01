package com.github.rschmitt.dynamicobject;


import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNull;

public class RecursionTest {
    @Test
    public void recursion() {
        LinkedList tail = DynamicObject.newInstance(LinkedList.class).value("3");
        LinkedList middle = DynamicObject.newInstance(LinkedList.class).value("2").next(tail);
        LinkedList head = DynamicObject.newInstance(LinkedList.class).value("1").next(middle);

        roundTrip(tail);
        roundTrip(middle);
        roundTrip(head);

        assertEquals("1", head.value());
        assertEquals("2", head.next().value());
        assertEquals("3", head.next().next().value());
        assertNull(head.next().next().next());
    }

    @Test
    public void taggedRecursion() {
        DynamicObject.registerTag(LinkedList.class, "LinkedList");
        try {
            recursion();
        } finally {
            DynamicObject.deregisterTag(LinkedList.class);
        }
    }

    @Test
    @Ignore
    public void registeringTheTagDoesNotAffectEqualityOfDeserializedInstances() {
        LinkedList obj1 = DynamicObject.deserialize("{:value \"1\", :next {:value \"2\", :next {:value \"3\"}}}", LinkedList.class);
        DynamicObject.registerTag(LinkedList.class, "LinkedList");
        LinkedList obj2 = DynamicObject.deserialize("#LinkedList{:value \"1\", :next #LinkedList{:value \"2\", :next #LinkedList{:value \"3\"}}}", LinkedList.class);
        DynamicObject.deregisterTag(LinkedList.class);

        LinkedList next = obj1.next().next();
        LinkedList next2 = obj1.next().next();
        assertEquals(next, next2);
        assertTrue(next.equals(next2));
        assertTrue(obj1.equals(obj2));
        assertEquals(obj1.next(), obj2.next());
        assertEquals(obj1, obj2);
        assertEquals(DynamicObject.serialize(obj1), DynamicObject.serialize(obj2));
    }

    private void roundTrip(LinkedList linkedList) {
        assertEquals(linkedList, DynamicObject.deserialize(DynamicObject.serialize(linkedList), LinkedList.class));
    }
}

interface LinkedList extends DynamicObject<LinkedList> {
    String value();

    LinkedList next();

    LinkedList value(String value);

    LinkedList next(LinkedList linkedList);
}