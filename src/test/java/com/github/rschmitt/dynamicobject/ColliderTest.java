package com.github.rschmitt.dynamicobject;

import com.github.rschmitt.collider.ClojureList;
import com.github.rschmitt.collider.ClojureMap;
import com.github.rschmitt.collider.ClojureSet;

import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;

import static com.github.rschmitt.collider.Collider.clojureList;
import static com.github.rschmitt.collider.Collider.clojureMap;
import static com.github.rschmitt.collider.Collider.clojureSet;
import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.fromFressianByteArray;
import static com.github.rschmitt.dynamicobject.DynamicObject.newInstance;
import static com.github.rschmitt.dynamicobject.DynamicObject.toFressianByteArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ColliderTest {
    static final Batch emptyBatch = newInstance(Batch.class);
    static final Instant inst = Instant.parse("1985-04-12T23:20:50.52Z");

    @BeforeClass
    public static void setup() {
        DynamicObject.registerTag(Batch.class, "batch");
    }

    @Test
    public void clojureMapDeserialization() throws Exception {
        Batch batch = deserialize("{:map {\"key\" 3}}", Batch.class);

        ClojureMap<String, Long> map = batch.map();

        assertEquals(3, map.get("key").intValue());
        assertTrue(map.dissoc("key").isEmpty());
        fressianRoundTrip(batch);
    }

    @Test
    public void clojureSetDeserialization() throws Exception {
        Batch batch = deserialize("{:set #{#inst \"1985-04-12T23:20:50.520-00:00\"}}", Batch.class);

        ClojureSet<Instant> set = batch.set();

        assertTrue(set.contains(inst));
        fressianRoundTrip(batch);
    }

    @Test
    public void clojureListDeserialization() throws Exception {
        Batch batch = deserialize("{:list [\"a\" nil \"c\"]}", Batch.class);

        ClojureList<String> list = batch.list();

        assertEquals("a", list.get(0));
        assertEquals(null, list.get(1));
        assertEquals("c", list.get(2));
        assertEquals("d", list.append("d").get(3));
        fressianRoundTrip(batch);
    }

    @Test
    public void clojureMapBuilders() throws Exception {
        ClojureMap<String, Long> map = clojureMap("key", 3L);

        Batch batch = emptyBatch.map(map);

        assertEquals(map, batch.map());
        fressianRoundTrip(batch);
    }

    @Test
    public void clojureSetBuilders() throws Exception {
        ClojureSet<Instant> set = clojureSet(inst);

        Batch batch = emptyBatch.set(set);

        assertEquals(set, batch.set());
        fressianRoundTrip(batch);
    }

    @Test
    public void clojureListBuilders() throws Exception {
        ClojureList<String> list = clojureList("a", null, "c");

        Batch batch = emptyBatch.list(list);

        assertEquals(list, batch.list());
        fressianRoundTrip(batch);
    }

    @Test
    public void mapBuilders() throws Exception {
        ClojureMap<String, Long> map = clojureMap("key", 3L);

        Batch batch = emptyBatch.map2(map);

        assertEquals(map, batch.map());
        fressianRoundTrip(batch);
    }

    @Test
    public void setBuilders() throws Exception {
        ClojureSet<Instant> set = clojureSet(inst);

        Batch batch = emptyBatch.set2(set);

        assertEquals(set, batch.set());
        fressianRoundTrip(batch);
    }

    @Test
    public void listBuilders() throws Exception {
        ClojureList<String> list = clojureList("a", null, "c");

        Batch batch = emptyBatch.list2(list);

        assertEquals(list, batch.list());
        fressianRoundTrip(batch);
    }

    private void fressianRoundTrip(Batch batch) {
        Batch actual = fromFressianByteArray(toFressianByteArray(batch));

        assertEquals(batch, actual);
        assertEquals(batch.map(), actual.map());
        assertEquals(batch.set(), actual.set());
        assertEquals(batch.list(), actual.list());
    }

    public interface Batch extends DynamicObject<Batch> {
        ClojureMap<String, Long> map();
        ClojureSet<Instant> set();
        ClojureList<String> list();

        Batch map(ClojureMap<String, Long> map);
        Batch set(ClojureSet<Instant> set);
        Batch list(ClojureList<String> list);

        @Key(":map") Batch map2(ClojureMap<String, Long> map);
        @Key(":set") Batch set2(ClojureSet<Instant> set);
        @Key(":list") Batch list2(ClojureList<String> list);
    }
}
