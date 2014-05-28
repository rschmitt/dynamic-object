import clojure.lang.*;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

import static java.lang.String.format;

public class DynamicObjectTest {
    static final String SIMPLE_SCHEMA_EDN = "{:str \"expected value\", :i 4, :d 3.14, :f 3.14, :lng 1234567890, :shrt 4}";
    static final String NESTED_SCHEMA_EDN = format("{:version 1, :simple %s}", SIMPLE_SCHEMA_EDN);

    @Test
    public void getSimpleFields() {
        SimpleSchema simpleSchema = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);

        assertEquals("expected value", simpleSchema.str());
        assertEquals(4, simpleSchema.shrt());
        assertEquals(4, simpleSchema.i());
        assertEquals(1234567890L, simpleSchema.lng());
        assertEquals(3.14, simpleSchema.d(), 0.001);
        assertEquals(3.14, simpleSchema.f(), 0.001);
    }

    @Test
    public void nesting() {
        SimpleSchema simpleSchema = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);
        NestedSchema nestedSchema = DynamicObject.deserialize(NESTED_SCHEMA_EDN, NestedSchema.class);

        assertEquals(1, nestedSchema.version());
        assertEquals(simpleSchema, nestedSchema.simple());
    }

    @Test
    public void basicRoundTrip() {
        SimpleSchema simpleSchema = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);

        assertEquals(SIMPLE_SCHEMA_EDN, simpleSchema.toString());
    }

    @Test
    public void getType() {
        SimpleSchema simpleSchema = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);
        assertEquals(simpleSchema.getType(), SimpleSchema.class);
    }

    @Test
    public void getMap() {
        SimpleSchema simpleSchema = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);
        IPersistentMap map = (IPersistentMap) EdnReader.readString(SIMPLE_SCHEMA_EDN, PersistentHashMap.EMPTY);
        assertEquals(map, simpleSchema.getMap());
    }

    @Test
    public void assoc() {
        SimpleSchema initial = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);
        SimpleSchema changed = initial.assoc("str", "new value");

        assertEquals("new value", changed.str());

        SimpleSchema changedBack = changed.assoc("str", "expected value");
        assertEquals(initial, changedBack);
    }

    @Test
    public void equalsAndHashCode() {
        SimpleSchema instance1 = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);
        SimpleSchema instance2 = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);

        assertTrue(instance1 == instance1);
        assertFalse(instance1 == instance2);
        assertTrue(instance1.equals(instance1));
        assertTrue(instance1.equals(instance2));
        assertEquals(instance1, instance1);
        assertEquals(instance1, instance2);
        assertEquals(instance1.hashCode(), instance2.hashCode());
        assertFalse(instance1.assoc("key", "new-value").equals(instance2));
    }

    @Test
    public void unknownFields() {
        String edn = "{:str \"str\", :i 4, :d 3.14, :unknown \"unknown\"}";

        SimpleSchema withUnknowns = DynamicObject.deserialize(edn, SimpleSchema.class);
        SimpleSchema regular = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);

        assertFalse(withUnknowns.equals(regular));
        assertEquals(withUnknowns.getMap().valAt(getKeyword("unknown")), "unknown");
        assertEquals(edn, withUnknowns.toString());
    }

    @Test
    public void assocEx() {
        SimpleSchema initial = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);
        SimpleSchema assoced = initial.assocEx("new-field", "new-value");
        assertEquals("new-value", assoced.getMap().valAt(getKeyword("new-field")));
    }

    @Test(expected = RuntimeException.class)
    public void invalidAssocEx() {
        SimpleSchema simpleSchema = DynamicObject.deserialize(SIMPLE_SCHEMA_EDN, SimpleSchema.class);
        simpleSchema.assocEx("str", "str");
    }

    @Test
    public void without() {
        SimpleSchema simpleSchema = DynamicObject.deserialize("{:str \"value\"}", SimpleSchema.class);

        SimpleSchema empty = simpleSchema.without("str");

        assertEquals("{}", empty.toString());
    }

    @Test
    public void listOfStrings() {
        ListSchema listSchema = DynamicObject.deserialize("{:strings [\"one\" \"two\" \"three\"]}", ListSchema.class);
        List<String> stringList = listSchema.strings();
        assertEquals("one", stringList.get(0));
        assertEquals("two", stringList.get(1));
        assertEquals("three", stringList.get(2));
    }

    // This is just here to prove a point about Java<->Clojure interop.
    @Test
    public void listStream() {
        ListSchema listSchema = DynamicObject.deserialize("{:strings [\"one\" \"two\" \"three\"]}", ListSchema.class);
        List<String> stringList = listSchema.strings();

        List<Integer> collect = stringList.stream().map(x -> x.length()).collect(Collectors.toList());

        assertEquals(3, collect.get(0).intValue());
        assertEquals(3, collect.get(1).intValue());
        assertEquals(5, collect.get(2).intValue());
    }

    @Test
    public void setOfStrings() {
        SetSchema setSchema = DynamicObject.deserialize("{:strings #{\"one\" \"two\" \"three\"}}", SetSchema.class);
        Set<String> stringSet = setSchema.strings();
        assertEquals(3, stringSet.size());
        assertTrue(stringSet.contains("one"));
        assertTrue(stringSet.contains("two"));
        assertTrue(stringSet.contains("three"));
    }

    private Keyword getKeyword(String keyword) {
        return Keyword.intern(Symbol.intern(keyword));
    }
}

interface SimpleSchema extends DynamicObject<SimpleSchema> {
    short shrt();
    int i();
    long lng();
    float f();
    double d();
    String str();
}

interface NestedSchema extends DynamicObject<NestedSchema> {
    int version();
    SimpleSchema simple();
}

interface ListSchema extends DynamicObject<ListSchema> {
    List<String> strings();
}

interface SetSchema extends DynamicObject<SetSchema> {
    Set<String> strings();
}

/*
 * TODO:
 * Consider adding support for kebab-case (but note the possible impact on round-tripping)
 ** Consider adding support for a @Keyword annotation, so as to not be totally dependent on convention
 * Consider support for withers, e.g. String str() => SimpleSchema str(String str)
 * Consider supporting arbitrary default methods in subclasses of DynamicObject
 * Think about adding a validate() method that will be called before new instances are published
 * Add tests for Map support (should be covered by APersistentMap)
 */