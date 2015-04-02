package com.github.rschmitt.dynamicobject;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.fromFressianByteArray;
import static com.github.rschmitt.dynamicobject.DynamicObject.serialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.toFressianByteArray;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.fressian.Reader;
import org.fressian.Writer;
import org.fressian.handlers.ReadHandler;
import org.fressian.handlers.WriteHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExtensibilityTest {
    private static final String Edn = "#dh{:dumb [#MyDumbClass{:version 1, :str \"str\"}]}";

    @Before
    public void setup() {
        DynamicObject.registerType(DumbClass.class, new DumbClassTranslator());
        DynamicObject.registerTag(DumbClassHolder.class, "dh");
        DynamicObject.registerType(DumbClass.class, "dumb", new DumbClassReader(), new DumbClassWriter());
    }

    @After
    public void teardown() {
        DynamicObject.deregisterType(DumbClass.class);
        DynamicObject.deregisterTag(DumbClassHolder.class);
    }

    @Test
    public void roundTrip() {
        DumbClassHolder holder = deserialize(Edn, DumbClassHolder.class);

        String serialized = serialize(holder);

        assertEquals(Edn, serialized);
        assertEquals(new DumbClass(1, "str"), holder.dumb().get(0));
        assertEquals(holder, fromFressianByteArray(toFressianByteArray(holder)));
    }

    @Test
    public void serializeRegisteredType() {
        DumbClass dumbClass = new DumbClass(24, "twenty-four");

        String serialized = serialize(dumbClass);

        assertEquals("#MyDumbClass{:version 24, :str \"twenty-four\"}", serialized);
    }

    @Test
    public void deserializeRegisteredType() {
        String edn = "#MyDumbClass{:version 24, :str \"twenty-four\"}";

        DumbClass instance = deserialize(edn, DumbClass.class);

        assertEquals(new DumbClass(24, "twenty-four"), instance);
    }

    @Test
    public void prettyPrint() {
        DumbClassHolder holder = deserialize(Edn, DumbClassHolder.class);
        String expectedFormattedString = format("{:dumb [#MyDumbClass{:version 1, :str \"str\"}]}%n");
        assertEquals(expectedFormattedString, holder.toFormattedString());
    }

    @Test
    public void serializeBuiltinType() {
        assertEquals("true", serialize(true));
        assertEquals("false", serialize(false));
        assertEquals("25", serialize(25));
        assertEquals("\"asdf\"", serialize("asdf"));
    }

    // This is a DynamicObject that contains a regular POJO.
    public interface DumbClassHolder extends DynamicObject<DumbClassHolder> {
        List<DumbClass> dumb();
    }
}

// This is a translation class that functions as an Edn reader/writer for its associated POJO.
class DumbClassTranslator implements EdnTranslator<DumbClass> {
    @Override
    public DumbClass read(Object obj) {
        DumbClassProxy proxy = DynamicObject.wrap((Map) obj, DumbClassProxy.class);
        return new DumbClass(proxy.version(), proxy.str());
    }

    @Override
    public String write(DumbClass obj) {
        DumbClassProxy proxy = DynamicObject.newInstance(DumbClassProxy.class);
        proxy = proxy.str(obj.getStr());
        proxy = proxy.version(obj.getVersion());
        return serialize(proxy);
    }

    @Override
    public String getTag() {
        return "MyDumbClass"; // This is deliberately different from the class name.
    }

    public interface DumbClassProxy extends DynamicObject<DumbClassProxy> {
        long version();
        String str();

        DumbClassProxy version(long version);
        DumbClassProxy str(String str);
    }
}

class DumbClassReader implements ReadHandler {
    @Override
    public Object read(Reader r, Object tag, int componentCount) throws IOException {
        return new DumbClass(r.readInt(), (String) r.readObject());
    }
}

class DumbClassWriter implements WriteHandler {
    @Override
    public void write(Writer w, Object instance) throws IOException {
        DumbClass dumb = (DumbClass) instance;
        w.writeTag("dumb", 2);
        w.writeInt(dumb.getVersion());
        w.writeObject(dumb.getStr());
    }
}

// This is a POJO that has no knowledge of Edn.
class DumbClass {
    private final long version;
    private final String str;

    DumbClass(long version, String str) {
        this.version = version;
        this.str = str;
    }

    public long getVersion() {
        return version;
    }

    public String getStr() {
        return str;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DumbClass dumbClass = (DumbClass) o;

        return version == dumbClass.version && str.equals(dumbClass.str);
    }

    @Override
    public int hashCode() {
        int result = (int) (version ^ (version >>> 32));
        result = 31 * result + str.hashCode();
        return result;
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException("I'm a useless legacy toString() method that doesn't produce Edn!");
    }
}
