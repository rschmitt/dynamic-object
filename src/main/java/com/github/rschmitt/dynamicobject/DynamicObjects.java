package com.github.rschmitt.dynamicobject;

import java.io.PushbackReader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.github.rschmitt.dynamicobject.ClojureStuff.*;
import static java.lang.String.format;

public class DynamicObjects {
    private static volatile Object readers = EmptyMap;
    private static final ConcurrentHashMap<Class<?>, String> recordTagCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, EdnTranslatorAdapter<?>> translatorCache = new ConcurrentHashMap<>();

    static String serialize(Object obj) {
        if (obj instanceof DynamicObject)
            return (String) PrintString.invoke(((DynamicObject) obj).getMap());
        return (String) PrintString.invoke(obj);
    }

    static <T> T deserialize(String edn, Class<T> type) {
        return deserialize(new PushbackReader(new StringReader(edn)), type);
    }

    @SuppressWarnings("unchecked")
    static <T> T deserialize(PushbackReader streamReader, Class<T> type) {
        Object obj = Read.invoke(getReadersAsOptions(), streamReader);
        if (DynamicObject.class.isAssignableFrom(type))
            return wrap(obj, type);
        return (T) obj;
    }

    static <T extends DynamicObject<T>> Stream<T> deserializeStream(PushbackReader streamReader, Class<T> type) {
        Iterator<T> iterator = deserializeStreamToIterator(streamReader, type);
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.IMMUTABLE);
        return StreamSupport.stream(spliterator, false);
    }

    private static <T extends DynamicObject<T>> Iterator<T> deserializeStreamToIterator(PushbackReader streamReader, Class<T> type) {
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
    static <T> T wrap(Object map, Class<T> type) {
        Class<?> typeMetadata = Metadata.getTypeMetadata(map);
        if (typeMetadata != null && !type.equals(typeMetadata))
            throw new ClassCastException(String.format("Attempted to wrap a map tagged as %s in type %s",
                    typeMetadata.getSimpleName(), type.getSimpleName()));
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{type},
                new DynamicObjectInvocationHandler(map, type));
    }

    static <T extends DynamicObject<T>> T newInstance(Class<T> type) {
        return wrap(Metadata.withTypeMetadata(EmptyMap, type), type);
    }

    static synchronized <T> void registerType(Class<T> type, EdnTranslator<T> translator) {
        EdnTranslatorAdapter<T> adapter = new EdnTranslatorAdapter<>(translator);
        translatorCache.put(type, adapter);
        readers = Assoc.invoke(readers, cachedRead(translator.getTag()), adapter);
        defineMultimethod(type.getTypeName(), "DynamicObjects/invokeWriter", translator.getTag());
    }

    @SuppressWarnings("unchecked")
    static synchronized <T> void deregisterType(Class<T> type) {
        EdnTranslatorAdapter<T> adapter = (EdnTranslatorAdapter<T>) translatorCache.get(type);
        readers = Dissoc.invoke(readers, cachedRead(adapter.getTag()));
        RemoveMethod.invoke(PrintMethod, adapter);
        translatorCache.remove(type);
    }

    static synchronized <T extends DynamicObject<T>> void registerTag(Class<T> type, String tag) {
        recordTagCache.put(type, tag);
        readers = Assoc.invoke(readers, cachedRead(tag), new RecordReader<>(type));
        defineMultimethod(":" + type.getTypeName(), "RecordPrinter/printRecord", tag);
    }

    static synchronized <T extends DynamicObject<T>> void deregisterTag(Class<T> type) {
        String tag = recordTagCache.get(type);
        readers = Dissoc.invoke(readers, cachedRead(tag));
        recordTagCache.remove(type);

        Object dispatchVal = cachedRead(":" + type.getTypeName());
        RemoveMethod.invoke(PrintMethod, dispatchVal);
    }

    private static Object getReadersAsOptions() {
        return Assoc.invoke(EmptyMap, Readers, readers);
    }

    @SuppressWarnings("unused")
    public static Object invokeWriter(Object obj, Writer writer, String tag) {
        EdnTranslatorAdapter translator = (EdnTranslatorAdapter<?>) Get.invoke(readers, cachedRead(tag));
        return translator.invoke(obj, writer);
    }

    private static void defineMultimethod(String dispatchVal, String method, String arg) {
        String clojureCode = format("(defmethod print-method %s [o, ^java.io.Writer w] (com.github.rschmitt.dynamicobject.%s o w \"%s\"))",
                dispatchVal, method, arg);
        Eval.invoke(ReadString.invoke(clojureCode));
    }
}
