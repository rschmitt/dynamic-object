package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.*;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

public interface DynamicObject<T> {
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
        if (TranslatorRegistry.records.contains(o.getType()))
            return (String) var.invoke(o);
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
        Object obj = EdnReader.readString(edn, TranslatorRegistry.getReadersAsOptions());
        if (obj instanceof DynamicObject)
            return (T) obj;
        IPersistentMap map = (IPersistentMap) obj;
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
     * Register a reader tag for a DynamicObject type. This is useful for reading Edn representations of Clojure
     * records.
     */
    public static <T> void registerTag(Class clazz, String tag) {
        synchronized (DynamicObject.class) {
            registerType(clazz, new RecordTranslator<T>(tag, clazz));
            TranslatorRegistry.records.add(clazz);
        }
    }

    /**
     * Deregister the reader tag for the given DynamicObject type.
     */
    public static <T> void deregisterTag(Class clazz, String tag) {
        synchronized (DynamicObject.class) {
            deregisterType(clazz, new RecordTranslator<T>(tag, clazz));
            TranslatorRegistry.records.remove(clazz);
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

class RecordTranslator<T> extends EdnTranslator<DynamicObject<T>> {
    private final String tag;
    private final Class<T> type;

    RecordTranslator(String tag, Class<T> type) {
        this.tag = tag;
        this.type = type;
    }

    @Override
    public DynamicObject read(Object obj) {
        return DynamicObject.wrap((IPersistentMap) obj, (Class<DynamicObject>) type);
    }

    @Override
    public String write(DynamicObject obj) {
        IFn var = Clojure.var("clojure.core", "pr-str");
        return (String) var.invoke(obj.getMap());
    }

    @Override
    public String getTag() {
        return tag;
    }
}

class TranslatorRegistry {
    static volatile IPersistentMap readers = PersistentHashMap.EMPTY;
    static final HashSet<Class> records = new HashSet<>();

    static IPersistentMap getReadersAsOptions() {
        return PersistentHashMap.EMPTY.assoc(Keyword.intern("readers"), readers);
    }
}
