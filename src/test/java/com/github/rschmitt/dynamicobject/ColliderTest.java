package com.github.rschmitt.dynamicobject;

import static com.github.rschmitt.collider.Collider.clojureList;
import static com.github.rschmitt.collider.Collider.clojureMap;
import static com.github.rschmitt.collider.Collider.clojureSet;
import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static com.github.rschmitt.dynamicobject.DynamicObject.fromFressianByteArray;
import static com.github.rschmitt.dynamicobject.DynamicObject.newInstance;
import static com.github.rschmitt.dynamicobject.DynamicObject.toFressianByteArray;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.rschmitt.collider.ClojureList;
import com.github.rschmitt.collider.ClojureMap;
import com.github.rschmitt.collider.ClojureSet;

public class ColliderTest {
    static final Batch emptyBatch = newInstance(Batch.class);
    static final Instant inst = Instant.parse("1985-04-12T23:20:50.52Z");

    @BeforeAll
    public static void setup() {
        DynamicObject.registerTag(Batch.class, "batch");
    }

    @Test
    public void clojureMapDeserialization() throws Exception {
        Batch batch = deserialize("{:map {\"key\" 3}}", Batch.class);

        ClojureMap<String, Integer> map = batch.map();

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

        ClojureList<Optional<String>> list = batch.list();

        assertEquals(of("a"), list.get(0));
        assertEquals(empty(), list.get(1));
        assertEquals(of("c"), list.get(2));
        assertEquals(of("d"), list.append(of("d")).get(3));
        fressianRoundTrip(batch);
    }

    @Test
    public void clojureMapBuilders() throws Exception {
        ClojureMap<String, Integer> map = clojureMap("key", 3);

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
        ClojureList<Optional<String>> list = clojureList(of("a"), empty(), of("c"));

        Batch batch = emptyBatch.list(list);

        assertEquals(list, batch.list());
        fressianRoundTrip(batch);
    }

    @Test
    public void mapBuilders() throws Exception {
        ClojureMap<String, Integer> map = clojureMap("key", 3);

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
        ClojureList<Optional<String>> list = clojureList(of("a"), empty(), of("c"));

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
        ClojureMap<String, Integer> map();
        ClojureSet<Instant> set();
        ClojureList<Optional<String>> list();

        Batch map(Map<String, Integer> map);
        Batch set(Set<Instant> set);
        Batch list(List<Optional<String>> list);

        @Key(":map") Batch map2(ClojureMap<String, Integer> map);
        @Key(":set") Batch set2(ClojureSet<Instant> set);
        @Key(":list") Batch list2(ClojureList<Optional<String>> list);
    }
}
