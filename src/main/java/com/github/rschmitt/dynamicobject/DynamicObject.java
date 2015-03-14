package com.github.rschmitt.dynamicobject;

import org.fressian.handlers.ReadHandler;
import org.fressian.handlers.WriteHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackReader;
import java.io.Writer;
import java.util.Map;
import java.util.function.BiFunction;
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
     * Lazily deserialize a stream of Fressian-encoded values as the given type.
     */
    static <T> Stream<T> deserializeFressianStream(InputStream is, Class<T> type) {
        return DynamicObjects.deserializeFressianStream(is, type);
    }

    /**
     * Serialize the {@code o} to {@code os}, using the Fressian binary serialization format. A Fressian footer
     * (containing an Adler32 checksum of the data) will be written, and the supplied OutputStream will be closed.
     */
    static void serializeToFressian(Object o, OutputStream os) {
        DynamicObjects.serializeToFressian(o, os);
    }

    /**
     * Read a Fressian-serialized object from the supplied InputStream. After the object is read, the Fressian footer
     * will be validated and {@code is} will be closed.
     */
    static <T> T deserializeFromFressian(InputStream is) {
        return DynamicObjects.deserializeFromFressian(is);
    }

    /**
     * Serialize {@code o} to binary Fressian data.
     */
    static byte[] toFressianByteArray(Object o) {
        return DynamicObjects.toFressianByteArray(o);
    }

    /**
     * Deserialize and return the Fressian-encoded object in {@code bytes}.
     */
    static <T> T fromFressianByteArray(byte[] bytes) {
        return DynamicObjects.fromFressianByteArray(bytes);
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
     * Register a {@link org.fressian.handlers.ReadHandler} and {@link org.fressian.handlers.WriteHandler} to enable
     * instances of {@code type} to be serialized to and deserialized from Fressian data.
     */
    static void registerType(Class type, String tag, ReadHandler readHandler, WriteHandler writeHandler) {
        DynamicObjects.registerType(type, tag, readHandler, writeHandler);
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

    /**
     * Specify a default reader, which is a function that will be called when any unknown reader tags are encountered.
     * The function will be passed the reader tag (as a string) and the tagged Edn element, and can return whatever it
     * wants.
     * <p/>
     * DynamicObject comes with a built-in default reader for unknown elements, which returns an instance of {@link
     * com.github.rschmitt.dynamicobject.Unknown}, which simply captures the (tag, element) tuple from the Edn reader.
     * The {@code Unknown} class is handled specially during serialization so that unknown elements can be serialized
     * correctly; this allows unknown types to be passed through transparently.
     * <p/>
     * To disable the default reader, call {@code DynamicObject.setDefaultReader(null)}. This will cause the reader to
     * throw an exception if unknown reader tags are encountered.
     */
    static <T> void setDefaultReader(BiFunction<String, Object, T> reader) {
        DynamicObjects.setDefaultReader(reader);
    }

    /**
     * Obtain a Fressian {@link org.fressian.handlers.ReadHandler} for the specified DynamicObject {@code type}. This
     * is only recommended if you want to roll your own {@link org.fressian.FressianReader} for maximum flexibility.
     * Otherwise, it is recommended to use {@link #fromFressianByteArray} or {@link #deserializeFromFressian} instead.
     */
    static <T extends DynamicObject<T>> ReadHandler getFressianReadHandler(String tag, Class<T> type) {
        return new FressianReadHandler(type, tag);
    }

    /**
     * Obtain a Fressian {@link org.fressian.handlers.WriteHandler} for the specified DynamicObject {@code type}. This
     * is only recommended if you want to roll your own {@link org.fressian.FressianWriter} for maximum flexibility.
     * Otherwise, it is recommended to use {@link #toFressianByteArray} or {@link #serializeToFressian} instead.
     */
    static <T extends DynamicObject<T>> WriteHandler getFressianWriteHandler(String tag, Class<T> type) {
        return new FressianWriteHandler(type, tag);
    }
}
