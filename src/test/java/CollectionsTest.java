import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CollectionsTest {
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
}

interface ListSchema extends DynamicObject<ListSchema> {
    List<String> strings();
}

interface SetSchema extends DynamicObject<SetSchema> {
    Set<String> strings();
}
