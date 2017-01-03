package com.github.rschmitt.dynamicobject;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackReader;
import java.io.Writer;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.fressian.FressianReader;
import org.fressian.FressianWriter;
import org.fressian.handlers.ReadHandler;
import org.fressian.handlers.WriteHandler;

import com.github.rschmitt.dynamicobject.internal.EdnSerialization;
import com.github.rschmitt.dynamicobject.internal.FressianSerialization;
import com.github.rschmitt.dynamicobject.internal.Instances;
import com.github.rschmitt.dynamicobject.internal.Serialization;

public interface DynamicObject<D extends DynamicObject<D>> extends Map {
    /**
     * @return the underlying Clojure map backing this instance. Downcasting the return value of this method to any
     *         particular Java type (e.g. IPersistentMap) is not guaranteed to work with future versions of Clojure.
     */
    Map getMap();
    
    /**
     * @return the apparent type of this instance. Note that {@code getClass} will return the class of the interface
     *         proxy and not the interface itself.
     */
    Class<D> getType();
    
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
    D merge(D other);
    
    /**
     * Recursively compares this instance with {@code other}, returning a new instance containing all of the common
     * elements of both {@code this} and {@code other}. Maps and lists are compared recursively; everything else,
     * including sets, strings, and POJOs, is treated atomically.
     * <p/>
     * Equivalent to: {@code (nth (clojure.data/diff this other) 2)}
     */
    D intersect(D other);
    
    /**
     * Recursively compares this instance with {@code other}, similar to {@link #intersect}, but returning the fields that
     * are unique to {@code this}. Uses the same recursion strategy as {@code intersect}.
     * <p/>
     * Equivalent to: {@code (nth (clojure.data/diff this other) 0)}
     */
    D subtract(D other);
    
    /**
     * Validate that all fields annotated with @Required are non-null, and that all present fields are of the correct
     * type. Returns the validated instance unchanged, which allows the validate method to be called at the end of a
     * fluent builder chain.
     */
    D validate();
    
    /**
     * Post-deserialization hook. The intended use of this method is to facilitate format upgrades. For example, if a
     * new field has been added to a DynamicObject schema, this method can be implemented to add that field (with
     * some appropriate default value) to older data upon deserialization.
     * <p/>
     * If not implemented, this method does nothing.
     * <p/>
     * @deprecated This method is experimental.
     */
    @Deprecated
    D afterDeserialization();
    
    /**
     * Serialize the given object to Edn. Any {@code EdnTranslator}s that have been registered through
     * {@link DynamicObject#registerType} will be invoked as needed.
     */
    static String serialize(Object o) {
        return EdnSerialization.serialize(o);
    }
    
    static void serialize(Object o, Writer w) {
        EdnSerialization.serialize(o, w);
    }
    
    /**
     * Deserializes a DynamicObject or registered type from a String.
     *
     * @param edn The Edn representation of the object.
     * @param type The type of class to deserialize. Must be an interface that extends DynamicObject.
     */
    static <T> T deserialize(String edn, Class<T> type) {
        return EdnSerialization.deserialize(edn, type);
    }
    
    /**
     * Lazily deserialize a stream of top-level Edn elements as the given type.
     */
    static <T> Stream<T> deserializeStream(PushbackReader streamReader, Class<T> type) {
        return EdnSerialization.deserializeStream(streamReader, type);
    }
    
    /**
     * Serialize a single object {@code o} to binary Fressian data.
     */
    static byte[] toFressianByteArray(Object o) {
        return FressianSerialization.toFressianByteArray(o);
    }
    
    /**
     * Deserialize and return the Fressian-encoded object in {@code bytes}.
     */
    static <T> T fromFressianByteArray(byte[] bytes) {
        return FressianSerialization.fromFressianByteArray(bytes);
    }
    
    /**
     * Create a {@link FressianReader} instance to read from {@code is}. The reader will be created with support for all
     * the basic Java and Clojure types, all DynamicObject types registered by calling {@link #registerTag(Class,
     * String)}, and any other types registered by calling {@link #registerType(Class, String, ReadHandler,
     * WriteHandler)}. If {@code validateChecksum} is true, the data will be checksummed as it is read; this checksum
     * can later be compared to the expected checksum in the Fressian footer by calling {@link
     * FressianReader#validateFooter()}.
     */
    static FressianReader createFressianReader(InputStream is, boolean validateChecksum) {
        return FressianSerialization.createFressianReader(is, validateChecksum);
    }
    
    /**
     * Create a {@link FressianWriter} instance to write to {@code os}. The writer will be created with support for all
     * the basic Java and Clojure types, all DynamicObject types registered by calling {@link #registerTag(Class,
     * String)}, and any other types registered by calling {@link #registerType(Class, String, ReadHandler,
     * WriteHandler)}. If desired, a Fressian footer (containing an Adler32 checksum of all data written) can be written
     * by calling {@link FressianWriter#writeFooter()}.
     */
    static FressianWriter createFressianWriter(OutputStream os) {
        return FressianSerialization.createFressianWriter(os);
    }
    
    /**
     * Lazily deserialize a stream of Fressian-encoded values as the given type. A Fressian footer, if encountered, will
     * be validated.
     */
    static <T> Stream<T> deserializeFressianStream(InputStream is, Class<T> type) {
        return FressianSerialization.deserializeFressianStream(is, type);
    }
    
    /**
     * Use the supplied {@code map} to back an instance of {@code type}.
     */
    static <D extends DynamicObject<D>> D wrap(Map map, Class<D> type) {
        return Instances.wrap(map, type);
    }
    
    /**
     * Create a "blank" instance of {@code type}, backed by an empty Clojure map. All fields will be null.
     */
    static <D extends DynamicObject<D>> D newInstance(Class<D> type) {
        return Instances.newInstance(type);
    }
    
    /**
     * Register an {@link EdnTranslator} to enable instances of {@code type} to be serialized to and deserialized from
     * Edn using reader tags.
     */
    static <T> void registerType(Class<T> type, EdnTranslator<T> translator) {
        EdnSerialization.registerType(type, translator);
    }
    
    /**
     * Register a {@link org.fressian.handlers.ReadHandler} and {@link org.fressian.handlers.WriteHandler} to enable
     * instances of {@code type} to be serialized to and deserialized from Fressian data.
     */
    static void registerType(Class type, String tag, ReadHandler readHandler, WriteHandler writeHandler) {
        FressianSerialization.registerType(type, tag, readHandler, writeHandler);
    }
    
    /**
     * Deregister the given {@code translator}. After this method is invoked, it will no longer be possible to read or
     * write instances of {@code type} unless another translator is registered.
     */
    static <T> void deregisterType(Class<T> type) {
        Serialization.deregisterType(type);
    }
    
    /**
     * Register a reader tag for a DynamicObject type. This is useful for reading Edn representations of Clojure
     * records.
     */
    static <D extends DynamicObject<D>> void registerTag(Class<D> type, String tag) {
        Serialization.registerTag(type, tag);
    }
    
    /**
     * Deregister the reader tag for the given DynamicObject type.
     */
    static <D extends DynamicObject<D>> void deregisterTag(Class<D> type) {
        Serialization.deregisterTag(type);
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
        EdnSerialization.setDefaultReader(reader);
    }
}
