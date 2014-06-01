package com.github.rschmitt.dynamicobject;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
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