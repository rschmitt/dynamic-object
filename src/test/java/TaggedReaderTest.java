import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TaggedReaderTest {
    @Before
    public void setup() {
        DynamicObject.registerType(DumbClass.class, new DumbClassTranslator());
    }

    @After
    public void teardown() {
        DynamicObject.deregisterType(DumbClass.class, new DumbClassTranslator());
    }

    @Test
    public void roundTrip() {
        String edn = "{:dumb #MyDumbClass {:version 1, :str \"str\"}}";

        DumbClassHolder deserialized = DynamicObject.deserialize(edn, DumbClassHolder.class);
        String serialized = DynamicObject.serialize(deserialized);

        assertEquals(edn, serialized);
        assertEquals(new DumbClass(1, "str"), deserialized.dumb());
    }
}

// This is a DynamicObject that contains a regular POJO.
interface DumbClassHolder extends DynamicObject<DumbClass> {
    DumbClass dumb();
}

// This is a translation class that functions as an Edn reader/writer for its associated POJO.
class DumbClassTranslator extends EdnTranslator<DumbClass> {
    @Override
    public DumbClass read(Object obj) {
        IPersistentMap map = (IPersistentMap) obj;
        long version = (Long) map.valAt(Keyword.intern("version"));
        String str = (String) map.valAt(Keyword.intern("str"));
        return new DumbClass(version, str);
    }

    @Override
    public String write(DumbClass obj) {
        return String.format("{:version %d, :str \"%s\"}", obj.getVersion(), obj.getStr());
    }

    @Override
    public String getTag() {
        return "MyDumbClass"; // This is deliberately different from the class name.
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

        if (version != dumbClass.version) return false;
        if (!str.equals(dumbClass.str)) return false;

        return true;
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