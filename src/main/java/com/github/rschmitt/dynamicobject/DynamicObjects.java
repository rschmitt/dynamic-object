package com.github.rschmitt.dynamicobject;

import java.io.PushbackReader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.rschmitt.dynamicobject.ClojureStuff.*;
import static java.lang.String.format;

public class DynamicObjects {
    private static volatile Object readers = EMPTY_MAP;
    private static final ConcurrentHashMap<Class<?>, String> recordTagCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, EdnTranslatorAdapter<?>> translatorCache = new ConcurrentHashMap<>();

    static String serialize(Object obj) {
        if (translatorCache.containsKey(obj.getClass()))
            return (String) PRINT_STRING.invoke(obj);
        if (obj instanceof DynamicObject)
            return (String) PRINT_STRING.invoke(((DynamicObject) obj).getMap());
        throw new UnsupportedOperationException("Unable to serialize type " + obj.getClass().getComponentType());
    }

    static <T extends DynamicObject<T>> T deserialize(String edn, Class<T> type) {
        return deserialize(new PushbackReader(new StringReader(edn)), type);
    }

    @SuppressWarnings("unchecked")
    static <T extends DynamicObject<T>> T deserialize(PushbackReader streamReader, Class<T> type) {
        Object obj = READ.invoke(getReadersAsOptions(), streamReader);
        return wrap(obj, type);
    }

    static <T extends DynamicObject<T>> Iterator<T> deserializeStream(PushbackReader streamReader, Class<T> type) {
        return new Iterator<T>() {
            private T stash = null;

            @Override
            public boolean hasNext() {
                if (stash != null)
                    return true;
                try {
                    stash = next();
                    return true;
                } catch (RuntimeException ex) {
                    return false;
                }
            }

            @Override
            public T next() {
                if (stash != null) {
                    T ret = stash;
                    stash = null;
                    return ret;
                }
                return deserialize(streamReader, type);
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <T extends DynamicObject<T>> T wrap(Object map, Class<T> type) {
        Class<?> typeMetadata = Metadata.getTypeMetadata(map);
        if (typeMetadata != null && !type.equals(typeMetadata))
            throw new ClassCastException(String.format("Attempted to wrap a map tagged as %s in type %s",
                    typeMetadata.getSimpleName(), type.getSimpleName()));
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
        return wrap(Metadata.withTypeMetadata(EMPTY_MAP, type), type);
    }

    static synchronized <T> void registerType(Class<T> type, EdnTranslator<T> translator) {
        EdnTranslatorAdapter<T> adapter = new EdnTranslatorAdapter<>(translator);
        translatorCache.put(type, adapter);
        readers = ASSOC.invoke(readers, cachedRead(translator.getTag()), adapter);
        defineMultimethod(type.getTypeName(), "DynamicObjects/invokeWriter", translator.getTag());
    }

    @SuppressWarnings("unchecked")
    static synchronized <T> void deregisterType(Class<T> type) {
        EdnTranslatorAdapter<T> adapter = (EdnTranslatorAdapter<T>) translatorCache.get(type);
        readers = DISSOC.invoke(readers, cachedRead(adapter.getTag()));
        REMOVE_METHOD.invoke(PRINT_METHOD, adapter);
        translatorCache.remove(type);
    }

    static synchronized <T extends DynamicObject<T>> void registerTag(Class<T> type, String tag) {
        recordTagCache.put(type, tag);
        readers = ASSOC.invoke(readers, cachedRead(tag), new RecordReader<>(type));
        defineMultimethod(":" + type.getTypeName(), "RecordPrinter/printRecord", tag);
    }

    static synchronized <T extends DynamicObject<T>> void deregisterTag(Class<T> type) {
        String tag = recordTagCache.get(type);
        readers = DISSOC.invoke(readers, cachedRead(tag));
        recordTagCache.remove(type);

        Object dispatchVal = cachedRead(":" + type.getTypeName());
        REMOVE_METHOD.invoke(PRINT_METHOD, dispatchVal);
    }

    private static Object getReadersAsOptions() {
        return ASSOC.invoke(EMPTY_MAP, READERS, readers);
    }

    @SuppressWarnings("unused")
    public static Object invokeWriter(Object obj, Writer writer, String tag) {
        EdnTranslatorAdapter translator = (EdnTranslatorAdapter<?>) GET.invoke(readers, cachedRead(tag));
        return translator.invoke(obj, writer);
    }

    private static void defineMultimethod(String dispatchVal, String method, String arg) {
        String clojureCode = format("(defmethod print-method %s [o, ^java.io.Writer w] (com.github.rschmitt.dynamicobject.%s o w \"%s\"))",
                dispatchVal, method, arg);
        EVAL.invoke(READ_STRING.invoke(clojureCode));
    }
}
