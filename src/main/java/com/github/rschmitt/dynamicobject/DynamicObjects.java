package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

public class DynamicObjects {
    private static final Object EMPTY_MAP = Clojure.read("{}");
    private static final Object TYPE = Clojure.read(":type");
    private static final IFn GET = Clojure.var("clojure.core", "get");
    private static final IFn ASSOC = Clojure.var("clojure.core", "assoc");
    private static final IFn DISSOC = Clojure.var("clojure.core", "dissoc");
    private static final IFn READ_STRING = Clojure.var("clojure.edn", "read-string");
    private static final IFn PRINT_STRING = Clojure.var("clojure.core", "pr-str");
    private static final IFn REMOVE_METHOD = Clojure.var("clojure.core", "remove-method");
    private static final IFn WITH_META = Clojure.var("clojure.core", "with-meta");
    private static final IFn DEREF = Clojure.var("clojure.core", "deref");
    private static final Object PRINT_METHOD = DEREF.invoke(Clojure.var("clojure.core", "print-method"));
    private static final IFn EVAL = Clojure.var("clojure.core", "eval");

    private static volatile Object readers = EMPTY_MAP;
    private static final ConcurrentHashMap<Class<?>, String> recordTagCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, EdnTranslator<?>> translatorCache = new ConcurrentHashMap<>();

    static <T extends DynamicObject<T>> String serialize(T o) {
        Class<T> type = o.getType();
        if (translatorCache.containsKey(type))
            return (String) PRINT_STRING.invoke(o);
        return (String) PRINT_STRING.invoke(o.getMap());
    }

    @SuppressWarnings("unchecked")
    static <T extends DynamicObject<T>> T deserialize(String edn, Class<T> type) {
        Object obj = READ_STRING.invoke(getReadersAsOptions(), edn);
        if (obj instanceof DynamicObject)
            return (T) obj;
        return wrap(obj, type);
    }

    @SuppressWarnings("unchecked")
    static <T extends DynamicObject<T>> T wrap(Object map, Class<T> type) {
        try {
            Constructor<MethodHandles.Lookup> lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            lookupConstructor.setAccessible(true);

            return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class<?>[]{type},
                    new DynamicObjectInvocationHandler<>(map, type, lookupConstructor));
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    static <T extends DynamicObject<T>> T newInstance(Class<T> type) {
        Object metadata = ASSOC.invoke(EMPTY_MAP, TYPE, Clojure.read(":" + type.getCanonicalName()));
        return wrap(WITH_META.invoke(EMPTY_MAP, metadata), type);
    }

    static synchronized <T> void registerType(Class<T> type, EdnTranslator<T> translator) {
        translatorCache.put(type, translator);
        readers = ASSOC.invoke(readers, Clojure.read(translator.getTag()), translator);
        defineMultimethod(type.getCanonicalName(), "DynamicObjects/invokeWriter", translator.getTag());
    }

    @SuppressWarnings("unchecked")
    static synchronized <T> void deregisterType(Class<T> type) {
        EdnTranslator<T> translator = (EdnTranslator<T>) translatorCache.get(type);
        readers = DISSOC.invoke(readers, Clojure.read(translator.getTag()));
        REMOVE_METHOD.invoke(PRINT_METHOD, translator);
        translatorCache.remove(type);
    }

    static synchronized <T extends DynamicObject<T>> void registerTag(Class<T> type, String tag) {
        recordTagCache.put(type, tag);
        readers = ASSOC.invoke(readers, Clojure.read(tag), new RecordReader<>(type));
        defineMultimethod(":" + type.getCanonicalName(), "RecordPrinter/printRecord", tag);
    }

    static synchronized <T extends DynamicObject<T>> void deregisterTag(Class<T> type) {
        String tag = recordTagCache.get(type);
        readers = DISSOC.invoke(readers, Clojure.read(tag));
        recordTagCache.remove(type);

        Object dispatchVal = Clojure.read(":" + type.getCanonicalName());
        REMOVE_METHOD.invoke(PRINT_METHOD, dispatchVal);
    }

    private static Object getReadersAsOptions() {
        return ASSOC.invoke(EMPTY_MAP, Clojure.read(":readers"), readers);
    }

    @SuppressWarnings("unused")
    public static Object invokeWriter(Object obj, Writer writer, String tag) {
        EdnTranslator translator = (EdnTranslator<?>) GET.invoke(readers, Clojure.read(tag));
        return translator.invoke(obj, writer);
    }

    private static void defineMultimethod(String dispatchVal, String method, String arg) {
        String clojureCode = format("(defmethod print-method %s [o, ^java.io.Writer w] (com.github.rschmitt.dynamicobject.%s o w \"%s\"))",
                dispatchVal, method, arg);
        EVAL.invoke(READ_STRING.invoke(clojureCode));
    }
}
