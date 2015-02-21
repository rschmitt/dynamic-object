package com.github.rschmitt.dynamicobject;


import org.junit.Before;
import org.junit.Test;

import static com.github.rschmitt.dynamicobject.DynamicObject.*;
import static org.junit.Assert.*;

public class RecursionTest {
    @Before
    public void setup() {
        try {
            DynamicObject.deregisterTag(LinkedList.class);
        } catch (NullPointerException ignore) { }
    }

    @Test
    public void recursion() {
        LinkedList tail = newInstance(LinkedList.class).value(3);
        LinkedList middle = newInstance(LinkedList.class).value(2).next(tail);
        LinkedList head = newInstance(LinkedList.class).value(1).next(middle);

        roundTrip(tail);
        roundTrip(middle);
        roundTrip(head);

        assertEquals(1, head.value());
        assertEquals(2, head.next().value());
        assertEquals(3, head.next().next().value());
        assertNull(head.next().next().next());

        assertEquals("{:next {:next {:value 3}, :value 2}, :value 1}", serialize(head));
        assertEquals("{:next {:value 3}, :value 2}", serialize(middle));
        assertEquals("{:value 3}", serialize(tail));
    }

    @Test
    public void taggedRecursion() {
        DynamicObject.registerTag(LinkedList.class, "LinkedList");

        LinkedList tail = newInstance(LinkedList.class).value(3);
        LinkedList middle = newInstance(LinkedList.class).value(2).next(tail);
        LinkedList head = newInstance(LinkedList.class).value(1).next(middle);

        roundTrip(tail);
        roundTrip(middle);
        roundTrip(head);
        assertEquals("#LinkedList{:value 3}", serialize(tail));
        assertEquals("#LinkedList{:next #LinkedList{:value 3}, :value 2}", serialize(middle));
        assertEquals("#LinkedList{:next #LinkedList{:next #LinkedList{:value 3}, :value 2}, :value 1}", serialize(head));
    }

    @Test
    public void registeringTheTagAddsItToSerializedOutput() {
        LinkedList tail = newInstance(LinkedList.class).value(3);
        LinkedList middle = newInstance(LinkedList.class).value(2).next(tail);
        LinkedList head = newInstance(LinkedList.class).value(1).next(middle);

        DynamicObject.registerTag(LinkedList.class, "LinkedList");

        roundTrip(tail);
        roundTrip(middle);
        roundTrip(head);
        assertEquals("#LinkedList{:value 3}", serialize(tail));
        assertEquals("#LinkedList{:next #LinkedList{:value 3}, :value 2}", serialize(middle));
        assertEquals("#LinkedList{:next #LinkedList{:next #LinkedList{:value 3}, :value 2}, :value 1}", serialize(head));
    }

    @Test
    public void deregisteringTheTagRemovesItFromSerializedOutput() {
        DynamicObject.registerTag(LinkedList.class, "LinkedList");

        LinkedList tail = newInstance(LinkedList.class).value(3);
        LinkedList middle = newInstance(LinkedList.class).value(2).next(tail);
        LinkedList head = newInstance(LinkedList.class).value(1).next(middle);

        DynamicObject.deregisterTag(LinkedList.class);

        roundTrip(tail);
        roundTrip(middle);
        roundTrip(head);

        assertEquals("{:next {:next {:value 3}, :value 2}, :value 1}", serialize(head));
        assertEquals("{:next {:value 3}, :value 2}", serialize(middle));
        assertEquals("{:value 3}", serialize(tail));
    }

    @Test
    public void registeringTheTagDoesNotAffectEqualityOfDeserializedInstances() {
        LinkedList obj1 = DynamicObject.deserialize("{:value 1, :next {:value 2, :next {:value 3}}}", LinkedList.class);
        DynamicObject.registerTag(LinkedList.class, "LinkedList");
        LinkedList obj2 = DynamicObject.deserialize("#LinkedList{:value 1, :next #LinkedList{:value 2, :next #LinkedList{:value 3}}}", LinkedList.class);
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
        assertEquals(linkedList, deserialize(serialize(linkedList), LinkedList.class));
    }
}

interface LinkedList extends DynamicObject<LinkedList> {
    long value();
    LinkedList next();

    LinkedList value(long value);
    LinkedList next(LinkedList linkedList);
}
