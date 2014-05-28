import clojure.lang.EdnReader;
import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import clojure.lang.Symbol;
import org.junit.Test;

import static clojure.lang.Keyword.intern;
import static org.junit.Assert.assertEquals;

public class TaggedReaderTest {
    @Test
    public void basicReading() {
        String edn = "#DumbClass {:version 1, :str \"str\"}";

        DumbClass dumbClass = (DumbClass) EdnReader.readString(edn, options(reader(DumbClass.SYMBOL, new DumbClassReader())));

        assertEquals(1, dumbClass.getVersion());
        assertEquals("str", dumbClass.getStr());
    }

    private IPersistentMap options(IPersistentMap readers) {
        return PersistentHashMap.EMPTY.assoc(intern(null, "readers"), readers);
    }

    private IPersistentMap reader(Symbol symbol, EdnTypeReader reader) {
        return PersistentHashMap.EMPTY.assoc(symbol, reader);
    }
}

class DumbClass {
    public static final Symbol SYMBOL = Symbol.intern("DumbClass");

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
}

class DumbClassReader extends EdnTypeReader {
    @Override
    public Object read(Object o) {
        IPersistentMap map = (IPersistentMap) o;
        long version = (Long) map.valAt(intern("version"));
        String str = (String) map.valAt(intern("str"));
        return new DumbClass(version, str);
    }
}

/*
 * TODO:
 * support Edn reader tags
 * investigate Edn *writing*--does Clojure rely purely on toString() for most types?
 */