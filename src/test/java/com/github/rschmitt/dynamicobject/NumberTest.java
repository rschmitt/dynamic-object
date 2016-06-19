package com.github.rschmitt.dynamicobject;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import clojure.lang.BigInt;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.fromFressianByteArray;
import static com.github.rschmitt.dynamicobject.DynamicObject.newInstance;
import static com.github.rschmitt.dynamicobject.DynamicObject.serialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.toFressianByteArray;
import static org.junit.Assert.assertEquals;

public class NumberTest {
    @Before
    public void setup() {
        DynamicObject.registerTag(ArbitraryPrecision.class, "ap");
    }

    @Test
    public void BigDecimal() {
        String edn = "#ap{:bigDecimal 3.14159M}";

        ArbitraryPrecision arbitraryPrecision = deserialize(edn, ArbitraryPrecision.class);

        assertEquals(edn, serialize(arbitraryPrecision));
        assertEquals(newInstance(ArbitraryPrecision.class).bigDecimal(new BigDecimal("3.14159")), arbitraryPrecision);
        binaryRoundTrip(arbitraryPrecision);
    }

    @Test
    public void BigInteger() {
        String edn = "#ap{:bigInteger 9234812039419082756912384500123N}";

        ArbitraryPrecision arbitraryPrecision = deserialize(edn, ArbitraryPrecision.class);

        assertEquals(edn, serialize(arbitraryPrecision));
        assertEquals(newInstance(ArbitraryPrecision.class).bigInteger(BigInt.fromBigInteger(new BigInteger("9234812039419082756912384500123"))), arbitraryPrecision);
        binaryRoundTrip(arbitraryPrecision);
    }

    private void binaryRoundTrip(Object expected) {
        Object actual = fromFressianByteArray(toFressianByteArray(expected));
        assertEquals(expected, actual);
    }

    public interface ArbitraryPrecision extends DynamicObject<ArbitraryPrecision> {
        BigDecimal bigDecimal();
        BigInt bigInteger();

        ArbitraryPrecision bigDecimal(BigDecimal bigDecimal);
        ArbitraryPrecision bigInteger(BigInt bigInteger);
    }
}
