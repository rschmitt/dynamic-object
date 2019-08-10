package com.github.rschmitt.dynamicobject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.github.rschmitt.dynamicobject.DynamicObject.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("deprecation")
public class DeserializationHookTest {
    @BeforeAll
    public static void setup() {
        registerTag(Registered.class, "Reg");
    }

    @Test
    public void ednDeserialization() {
        Registered r = (Registered) deserialize("#Reg{}", Object.class);

        assertEquals(42L, r.value());
    }

    @Test
    public void fressianDeserialization() throws Exception {
        Registered oldVersion = newInstance(Registered.class);

        byte[] bytes = toFressianByteArray(oldVersion);
        Registered newVersion = fromFressianByteArray(bytes);

        assertEquals(42, newVersion.value());
    }

    @Test
    public void hintedEdnDeserialization() throws Exception {
        Unregistered u = deserialize("{}", Unregistered.class);
        assertEquals(42L, u.value());
    }

    public interface Registered extends DynamicObject<Registered> {
        long value();

        Registered value(long value);

        @Override
        default Registered afterDeserialization() {
            return value(42);
        }
    }

    public interface Unregistered extends DynamicObject<Unregistered> {
        long value();

        Unregistered value(long value);

        @Override
        default Unregistered afterDeserialization() {
            return value(42);
        }
    }
}
