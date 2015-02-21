package com.github.rschmitt.dynamicobject;

import java.io.PushbackReader;
import java.io.Writer;
import java.util.Map;
import java.util.stream.Stream;

public interface DynamicObject<T extends DynamicObject<T>> {
    /**
     * @return the underlying Clojure map backing this instance. Downcasting the return value of this method to any
     * particular Java type (e.g. IPersistentMap) is not guaranteed to work with future versions of Clojure.
     */
    Map getMap();

    /**
     * @return the apparent type of this instance. Note that {@code getClass} will return the class of the interface
     * proxy and not the interface itself.
     */
    Class<T> getType();

    /**
     * Invokes clojure.pprint/pprint, which writes a pretty-printed representation of the object to the currently bound
     * value of *out*, which defaults to System.out (stdout).
     */
    void prettyPrint();

    /**
     * Like {@link DynamicObject#prettyPrint}, but returns the pretty-printed string instead of writing it to *out*.
     */
    String toFormattedString();

    /**
     * Return a copy of this instance with {@code other}'s fields merged in (nulls don't count). If a given field is
     * present in both instances, the fields in {@code other} will take precedence.
     * <p/>
     * Equivalent to: {@code (merge-with (fn [a b] (if (nil? b) a b)) this other)}
     */
    T merge(T other);

    /**
     * Recursively compares this instance with {@code other}, returning a new instance containing all of the common
     * elements of both {@code this} and {@code other}. Maps and lists are compared recursively; everything else,
     * including sets, strings, and POJOs, is treated atomically.
     * <p/>
     * Equivalent to: {@code (nth (clojure.data/diff this other) 2)}
     */
    T intersect(T other);

    /**
     * Recursively compares this instance with {@code other}, similar to {@link #intersect}, but returning the fields that
     * are unique to {@code this}. Uses the same recursion strategy as {@code intersect}.
     * <p/>
     * Equivalent to: {@code (nth (clojure.data/diff this other) 0)}
     */
    T subtract(T other);

    /**
     * Validate that all fields annotated with @Required are non-null, and that all present fields are of the correct
     * type. Returns the validated instance unchanged, which allows the validate method to be called at the end of a
     * fluent builder chain.
     */
    T validate();

    /**
     * Serialize the given object to Edn. Any {@code EdnTranslator}s that have been registered through
     * {@link DynamicObject#registerType} will be invoked as needed.
     */
    static String serialize(Object o) {
        return DynamicObjects.serialize(o);
    }

    static void serialize(Object o, Writer w) {
        DynamicObjects.serialize(o, w);
    }

    /**
     * Deserializes a DynamicObject or registered type from a String.
     *
     * @param edn  The Edn representation of the object.
     * @param type The type of class to deserialize. Must be an interface that extends DynamicObject.
     */
    static <T> T deserialize(String edn, Class<T> type) {
        return DynamicObjects.deserialize(edn, type);
    }

    /**
     * Lazily deserialize a stream of top-level Edn elements as the given type.
     */
    static <T> Stream<T> deserializeStream(PushbackReader streamReader, Class<T> type) {
        return DynamicObjects.deserializeStream(streamReader, type);
    }

    /**
     * Use the supplied {@code map} to back an instance of {@code type}.
     */
    static <T extends DynamicObject<T>> T wrap(Object map, Class<T> type) {
        return DynamicObjects.wrap(map, type);
    }

    /**
     * Create a "blank" instance of {@code type}, backed by an empty Clojure map. All fields will be null.
     */
    static <T extends DynamicObject<T>> T newInstance(Class<T> type) {
        return DynamicObjects.newInstance(type);
    }

    /**
     * Register an {@link EdnTranslator} to enable instances of {@code type} to be serialized to and deserialized from
     * Edn using reader tags.
     */
    static <T> void registerType(Class<T> type, EdnTranslator<T> translator) {
        DynamicObjects.registerType(type, translator);
    }

    /**
     * Deregister the given {@code translator}. After this method is invoked, it will no longer be possible to read or
     * write instances of {@code type} unless another translator is registered.
     */
    static <T> void deregisterType(Class<T> type) {
        DynamicObjects.deregisterType(type);
    }

    /**
     * Register a reader tag for a DynamicObject type. This is useful for reading Edn representations of Clojure
     * records.
     */
    static <T extends DynamicObject<T>> void registerTag(Class<T> type, String tag) {
        DynamicObjects.registerTag(type, tag);
    }

    /**
     * Deregister the reader tag for the given DynamicObject type.
     */
    static <T extends DynamicObject<T>> void deregisterTag(Class<T> type) {
        DynamicObjects.deregisterTag(type);
    }
}
