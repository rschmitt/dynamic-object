package com.github.rschmitt.dynamicobject;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.github.rschmitt.dynamicobject.DynamicObject.*;
import static com.github.rschmitt.dynamicobject.TestUtils.assertEquivalent;

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
        assertEquivalent("#Tagged{}", emptyTagged.toString());
        assertEquivalent("{}", emptyUntagged.toString());
        assertEquivalent("#Tagged{:untagged {}, :tagged #Tagged{}}", nestedTagged.toString());
        assertEquivalent("{:untagged {}, :tagged #Tagged{}}", nestedUntagged.toString());
    }

    @Test
    public void toFormattedStringTest() {
        assertEquivalent("#Tagged{}", emptyTagged.toFormattedString());
        assertEquivalent("{}", emptyUntagged.toFormattedString());
        assertEquivalent("#Tagged{:untagged {}, :tagged #Tagged{}}", nestedTagged.toFormattedString());
        assertEquivalent("{:untagged {}, :tagged #Tagged{}}", nestedUntagged.toFormattedString());
    }

    @Test
    public void serializeTest() {
        assertEquivalent("#Tagged{}", serialize(emptyTagged));
        assertEquivalent("{}", serialize(emptyUntagged));
        assertEquivalent("#Tagged{:untagged {}, :tagged #Tagged{}}", serialize(nestedTagged));
        assertEquivalent("{:untagged {}, :tagged #Tagged{}}", serialize(nestedUntagged));
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
