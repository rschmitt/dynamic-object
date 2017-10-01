package com.github.rschmitt.dynamicobject;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/*
 * This test exposes a bug where proxyCache#computeIfAbsent in Instances was being called
 * recursively, resulting in a non-terminating program (JDK-8062841) on some JDK versions. The call
 * to 'newInstance' causes DO to try to initialize the DynamicProxy for that type, which causes the
 * static initializer to run, which results in 'newInstance' being called to initialize the static
 * field, which results in a recursive call to proxyCache#computeIfAbsent, which results in
 * undefined behavior. The fix is simply to force class loading before attempting to create a
 * DynamicProxy.
 */
public class StaticFieldTest {
    @Test
    public void asdf() throws Exception {
        Holder holder = DynamicObject.newInstance(Holder.class);
        assertNull(holder.getMap().get("asdf"));
    }

    public interface Holder extends DynamicObject<Holder> {
        Holder holder = DynamicObject.newInstance(Holder.class);
    }
}
