package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import java.math.BigDecimal;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.newInstance;
import static com.github.rschmitt.dynamicobject.DynamicObject.serialize;
import static org.junit.Assert.assertEquals;

public class NumberTest {
    @Test
    public void BigDecimal() {
        String edn = "{:bigDecimal 3.14159M}";
        ArbitraryPrecision arbitraryPrecision = deserialize(edn, ArbitraryPrecision.class);
        assertEquals(edn, serialize(arbitraryPrecision));
        assertEquals(newInstance(ArbitraryPrecision.class).bigDecimal(new BigDecimal("3.14159")), arbitraryPrecision);
    }
}

interface ArbitraryPrecision extends DynamicObject<ArbitraryPrecision> {
    BigDecimal bigDecimal();

    ArbitraryPrecision bigDecimal(BigDecimal bigDecimal);
}