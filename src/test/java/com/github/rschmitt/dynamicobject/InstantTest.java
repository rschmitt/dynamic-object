package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import java.time.Instant;
import java.util.Date;

import static com.github.rschmitt.dynamicobject.DynamicObject.*;
import static org.junit.Assert.assertEquals;

public class InstantTest {
    @Test
    public void dateBuilder() {
        Date expected = Date.from(Instant.parse("1985-04-12T23:20:50.52Z"));

        TimeWrapper timeWrapper = newInstance(TimeWrapper.class).date(expected);

        assertEquals(expected, timeWrapper.date());
        assertEquals("{:date #inst \"1985-04-12T23:20:50.520-00:00\"}", serialize(timeWrapper));
    }

    @Test
    public void instantBuilder() {
        Instant expected = Instant.parse("1985-04-12T23:20:50.52Z");

        TimeWrapper timeWrapper = newInstance(TimeWrapper.class).instant(expected);

        assertEquals(expected, timeWrapper.instant());
        assertEquals("{:instant #inst \"1985-04-12T23:20:50.520-00:00\"}", serialize(timeWrapper));
    }

    @Test
    public void dateParser() {
        String edn = "{:date #inst \"1985-04-12T23:20:50.520-00:00\"}";
        Date expected = Date.from(Instant.parse("1985-04-12T23:20:50.52Z"));

        TimeWrapper timeWrapper = deserialize(edn, TimeWrapper.class);

        assertEquals(expected, timeWrapper.date());
    }

    @Test
    public void instantParser() {
        String edn = "{:instant #inst \"1985-04-12T23:20:50.520-00:00\"}";
        Instant expected = Instant.parse("1985-04-12T23:20:50.52Z");

        TimeWrapper timeWrapper = deserialize(edn, TimeWrapper.class);

        assertEquals(expected, timeWrapper.instant());
        assertEquals(edn, serialize(timeWrapper));
    }
}

interface TimeWrapper extends DynamicObject<TimeWrapper> {
    Date date();
    Instant instant();

    TimeWrapper date(Date date);
    TimeWrapper instant(Instant instant);
}
