package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.*;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

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
    public static <T extends DynamicObject<T>> String serialize(T o) {
        IFn prstr = Clojure.var("clojure.core", "pr-str");
        Class<T> type = o.getType();
        if (TranslatorRegistry.translatorCache.containsKey(type))
            return (String) prstr.invoke(o);
        IPersistentMap map = o.getMap();
        return (String) prstr.invoke(map);
    }

    /**
     * Deserializes a DynamicObject from a String.
     *
     * @param edn   The Edn representation of the object.
     * @param clazz The type of class to deserialize. Must be an interface that extends DynamicObject.
     */
    @SuppressWarnings("unchecked")
    public static <T extends DynamicObject<T>> T deserialize(String edn, Class<T> clazz) {
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
    public static <T extends DynamicObject<T>> T wrap(IPersistentMap map, Class<T> clazz) {
        try {
            Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            constructor.setAccessible(true);

            return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class<?>[]{clazz},
                    new DynamicObjectInvocationHandler<>(map, clazz, constructor));
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create a "blank" instance of {@code clazz}, backed by an empty {@link clojure.lang.IPersistentMap}. All fields
     * will be null.
     */
    public static <T extends DynamicObject<T>> T newInstance(Class<T> clazz) {
        IPersistentMap meta = PersistentHashMap.EMPTY.assoc(Keyword.intern("type"),
                Keyword.intern(clazz.getCanonicalName()));
        return wrap(PersistentHashMap.EMPTY.withMeta(meta), clazz);
    }

    /**
     * Register an {@link EdnTranslator} to enable instances of {@code clazz} to be serialized to and deserialized from
     * Edn using reader tags.
     */
    public static <T> void registerType(Class<T> clazz, EdnTranslator<T> translator) {
        synchronized (DynamicObject.class) {
            TranslatorRegistry.translatorCache.put(clazz, translator);

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
    @SuppressWarnings("unchecked")
    public static <T> void deregisterType(Class<T> clazz) {
        synchronized (DynamicObject.class) {
            EdnTranslator<T> translator = (EdnTranslator<T>) TranslatorRegistry.translatorCache.get(clazz);

            // uninstall reader
            TranslatorRegistry.readers = TranslatorRegistry.readers.without(Symbol.intern(translator.getTag()));

            // uninstall print-method multimethod
            Var varPrintMethod = (Var) Clojure.var("clojure.core", "print-method");
            MultiFn printMethod = (MultiFn) varPrintMethod.get();
            printMethod.removeMethod(clazz);

            TranslatorRegistry.translatorCache.remove(clazz);
        }
    }

    /**
     * Register a reader tag for a DynamicObject type. This is useful for reading Edn representations of Clojure
     * records.
     */
    public static <T extends DynamicObject<T>> void registerTag(Class<T> clazz, String tag) {
        synchronized (DynamicObject.class) {
            TranslatorRegistry.recordTagCache.put(clazz, tag);
            TranslatorRegistry.readers = TranslatorRegistry.readers
                    .assocEx(Symbol.intern(tag),
                            new AFn() {
                                @Override
                                public Object invoke(Object obj) {
                                    IObj mapWithMeta = (IObj) obj;
                                    IPersistentMap meta = mapWithMeta.meta();
                                    if (meta == null)
                                        meta = PersistentHashMap.EMPTY;
                                    IPersistentMap newMeta = meta.assoc(Keyword.intern("type"),
                                            Keyword.intern(clazz.getCanonicalName()));
                                    return mapWithMeta.withMeta(newMeta);
                                }
                            }
                    );


            Var varPrintMethod = (Var) Clojure.var("clojure.core", "print-method");
            MultiFn printMethod = (MultiFn) varPrintMethod.get();
            printMethod.addMethod(Keyword.intern(clazz.getCanonicalName()), new AFn() {
                @Override
                public Object invoke(Object arg1, Object arg2) {
                    Writer writer = (Writer) arg2;
                    IObj obj = (IObj) arg1;
                    obj = obj.withMeta(null);
                    try {
                        writer.write(String.format("#%s%s", tag, obj.toString()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                }
            });
        }
    }

    /**
     * Deregister the reader tag for the given DynamicObject type.
     */
    public static <T extends DynamicObject<T>> void deregisterTag(Class<T> clazz) {
        String tag = TranslatorRegistry.recordTagCache.get(clazz);
        TranslatorRegistry.readers = TranslatorRegistry.readers.without(Symbol.intern(tag));
        TranslatorRegistry.recordTagCache.remove(clazz);

        Var varPrintMethod = (Var) Clojure.var("clojure.core", "print-method");
        MultiFn printMethod = (MultiFn) varPrintMethod.get();
        printMethod.removeMethod(Keyword.intern(clazz.getCanonicalName()));
    }
}

class TranslatorRegistry {
    static volatile IPersistentMap readers = PersistentHashMap.EMPTY;
    static final ConcurrentHashMap<Class<?>, String> recordTagCache = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<Class<?>, EdnTranslator<?>> translatorCache = new ConcurrentHashMap<>();

    static IPersistentMap getReadersAsOptions() {
        return PersistentHashMap.EMPTY.assoc(Keyword.intern("readers"), readers);
    }
}
