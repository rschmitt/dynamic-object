package com.github.rschmitt.dynamicobject;

import clojure.lang.IPersistentMap;

public interface DynamicObject<T extends DynamicObject<T>> {
    /**
     * @return the underlying IPersistentMap backing this instance.
     */
    IPersistentMap getMap();

    /**
     * @return the apparent type of this instance. Note that {@code getClass} will return the class of the interface
     * proxy and not the interface itself.
     */
    Class<T> getType();

    /**
     * Return a persistent copy of this object with the new value associated with the given key.
     */
    T assoc(String key, Object value);

    /**
     * Same as {@link DynamicObject#assoc}, but throws an exception if the given key already exists.
     */
    T assocEx(String key, Object value);

    /**
     * Returns a persistent copy of this object dissoc the entry for the given key.
     */
    T dissoc(String key);

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
     * Serialize the given object to Edn. Any {@code EdnTranslator}s that have been registered through
     * {@link DynamicObject#registerType} will be invoked as needed.
     */
    public static <T extends DynamicObject<T>> String serialize(T o) {
        return DynamicObjects.serialize(o);
    }

    /**
     * Deserializes a DynamicObject from a String.
     *
     * @param edn   The Edn representation of the object.
     * @param clazz The type of class to deserialize. Must be an interface that extends DynamicObject.
     */
    public static <T extends DynamicObject<T>> T deserialize(String edn, Class<T> clazz) {
        return DynamicObjects.deserialize(edn, clazz);
    }

    /**
     * Use the supplied {@code map} to back an instance of {@code clazz}.
     */
    public static <T extends DynamicObject<T>> T wrap(IPersistentMap map, Class<T> clazz) {
        return DynamicObjects.wrap(map, clazz);
    }

    /**
     * Create a "blank" instance of {@code clazz}, backed by an empty {@link clojure.lang.IPersistentMap}. All fields
     * will be null.
     */
    public static <T extends DynamicObject<T>> T newInstance(Class<T> clazz) {
        return DynamicObjects.newInstance(clazz);
    }

    /**
     * Register an {@link EdnTranslator} to enable instances of {@code clazz} to be serialized to and deserialized from
     * Edn using reader tags.
     */
    public static <T> void registerType(Class<T> clazz, EdnTranslator<T> translator) {
        DynamicObjects.registerType(clazz, translator);
    }

    /**
     * Deregister the given {@code translator}. After this method is invoked, it will no longer be possible to read or
     * write instances of {@code clazz} unless another translator is registered.
     */
    public static <T> void deregisterType(Class<T> clazz) {
        DynamicObjects.deregisterType(clazz);
    }

    /**
     * Register a reader tag for a DynamicObject type. This is useful for reading Edn representations of Clojure
     * records.
     */
    public static <T extends DynamicObject<T>> void registerTag(Class<T> clazz, String tag) {
        DynamicObjects.registerTag(clazz, tag);
    }

    /**
     * Deregister the reader tag for the given DynamicObject type.
     */
    public static <T extends DynamicObject<T>> void deregisterTag(Class<T> clazz) {
        DynamicObjects.deregisterTag(clazz);
    }
}
