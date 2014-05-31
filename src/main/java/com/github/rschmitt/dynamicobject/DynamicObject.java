package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.*;

import java.lang.reflect.Proxy;

public interface DynamicObject<T> {
    /**
     * @return the underlying IPersistentMap backing this instance.
     */
    IPersistentMap getMap();

    /**
     * Return a persistent copy of this object with the new value associated with the given key.
     */
    T assoc(String key, Object value);

    /**
     * Same as {@link DynamicObject#assoc}, but throws an exception if the given key already exists.
     */
    T assocEx(String key, Object value);

    /**
     * Returns a persistent copy of this object without the entry for the given key.
     */
    T without(String key);

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
    public static String serialize(DynamicObject o) {
        IFn var = Clojure.var("clojure.core", "pr-str");
        IPersistentMap map = o.getMap();
        return (String) var.invoke(map);
    }

    /**
     * Deserializes a DynamicObject from a String.
     *
     * @param edn   The Edn representation of the object.
     * @param clazz The type of class to deserialize. Must be an interface that extends DynamicObject.
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(String edn, Class<T> clazz) {
        IPersistentMap map = (IPersistentMap) EdnReader.readString(edn, TranslatorRegistry.getReadersAsOptions());
        return wrap(map, clazz);
    }

    /**
     * Use the supplied {@code map} to back an instance of {@code clazz}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrap(IPersistentMap map, Class<T> clazz) {
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{clazz},
                new DynamicObjectInvocationHandler(map, clazz));
    }

    /**
     * Create a "blank" instance of {@code clazz}, backed by an empty {@link clojure.lang.IPersistentMap}. All fields
     * will be null.
     */
    public static <T> T newInstance(Class<T> clazz) {
        return wrap(PersistentHashMap.EMPTY, clazz);
    }

    /**
     * Register an {@link EdnTranslator} to enable instances of {@code clazz} to be serialized to and deserialized from
     * Edn using reader tags.
     */
    public static <T> void registerType(Class<T> clazz, EdnTranslator<T> translator) {
        synchronized (DynamicObject.class) {
            // install as a reader
            TranslatorRegistry.readers = TranslatorRegistry.readers.assocEx(Symbol.intern(translator.getTag()), translator);

            // install multimethod for writing
            Var varPrintMethod = (Var) Clojure.var("clojure.core", "print-method");
            MultiFn printMethod = (MultiFn) varPrintMethod.get();
            printMethod.addMethod(clazz, translator);
        }
    }

    /**
     * Deregister the given {@code translator}. After this method is invoked, it will no longer be possible to read or
     * write instances of {@code clazz} unless another translator is registered.
     */
    public static <T> void deregisterType(Class<T> clazz, EdnTranslator<T> translator) {
        synchronized (DynamicObject.class) {
            // uninstall reader
            TranslatorRegistry.readers = TranslatorRegistry.readers.without(Symbol.intern(translator.getTag()));

            // uninstall print-method multimethod
            Var varPrintMethod = (Var) Clojure.var("clojure.core", "print-method");
            MultiFn printMethod = (MultiFn) varPrintMethod.get();
            printMethod.removeMethod(clazz);
        }
    }
}

class TranslatorRegistry {
    static volatile IPersistentMap readers = PersistentHashMap.EMPTY;

    static IPersistentMap getReadersAsOptions() {
        return PersistentHashMap.EMPTY.assoc(Keyword.intern("readers"), readers);
    }
}
