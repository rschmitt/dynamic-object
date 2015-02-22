package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.github.rschmitt.dynamicobject.DynamicObject.*;
import static org.junit.Assert.assertEquals;

public class NumberTest {
    @Test
    public void BigDecimal() {
        String edn = "{:bigDecimal 3.14159M}";

        ArbitraryPrecision arbitraryPrecision = deserialize(edn, ArbitraryPrecision.class);

        assertEquals(edn, serialize(arbitraryPrecision));
        assertEquals(newInstance(ArbitraryPrecision.class).bigDecimal(new BigDecimal("3.14159")), arbitraryPrecision);
    }

    @Test
    public void BigInteger() {
        String edn = "{:bigInteger 9234812039419082756912384500123N}";

        ArbitraryPrecision arbitraryPrecision = deserialize(edn, ArbitraryPrecision.class);

        assertEquals(edn, serialize(arbitraryPrecision));
        assertEquals(newInstance(ArbitraryPrecision.class).bigInteger(new BigInteger("9234812039419082756912384500123")), arbitraryPrecision);
    }
}

interface ArbitraryPrecision extends DynamicObject<ArbitraryPrecision> {
    BigDecimal bigDecimal();
    BigInteger bigInteger();

    ArbitraryPrecision bigDecimal(BigDecimal bigDecimal);
    ArbitraryPrecision bigInteger(BigInteger bigInteger);
}
