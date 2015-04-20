package com.github.rschmitt.dynamicobject;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.github.rschmitt.dynamicobject.DynamicObject.*;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

public class PrintingTest {
    static Tagged emptyTagged;
    static Untagged emptyUntagged;
    static Tagged nestedTagged;
    static Untagged nestedUntagged;

    @BeforeClass
    public static void setup() {
        registerTag(Tagged.class, "Tagged");
        emptyTagged = newInstance(Tagged.class);
        emptyUntagged = newInstance(Untagged.class);

        nestedTagged = emptyTagged.tagged(emptyTagged).untagged(emptyUntagged);
        nestedUntagged = emptyUntagged.tagged(emptyTagged).untagged(emptyUntagged);
    }

    @Test
    public void toStringTest() {
        assertEquals("#Tagged{}", emptyTagged.toString());
        assertEquals("{}", emptyUntagged.toString());
        assertEquals("#Tagged{:untagged {}, :tagged #Tagged{}}", nestedTagged.toString());
        assertEquals("{:untagged {}, :tagged #Tagged{}}", nestedUntagged.toString());
    }

    @Test
    public void toFormattedStringTest() {
        assertEquals(format("#Tagged{}%n"), emptyTagged.toFormattedString());
        assertEquals(format("{}%n"), emptyUntagged.toFormattedString());
        assertEquals(format("#Tagged{:untagged {}, :tagged #Tagged{}}%n"), nestedTagged.toFormattedString());
        assertEquals(format("{:untagged {}, :tagged #Tagged{}}%n"), nestedUntagged.toFormattedString());
    }

    @Test
    public void serializeTest() {
        assertEquals("#Tagged{}", serialize(emptyTagged));
        assertEquals("{}", serialize(emptyUntagged));
        assertEquals("#Tagged{:untagged {}, :tagged #Tagged{}}", serialize(nestedTagged));
        assertEquals("{:untagged {}, :tagged #Tagged{}}", serialize(nestedUntagged));
    }
}

interface Tagged extends DynamicObject<Tagged> {
    @Key(":tagged") Tagged tagged(Tagged tagged);
    @Key(":untagged") Tagged untagged(Untagged untagged);
}

interface Untagged extends DynamicObject<Untagged> {
    @Key(":tagged") Untagged tagged(Tagged tagged);
    @Key(":untagged") Untagged untagged(Untagged untagged);
}
