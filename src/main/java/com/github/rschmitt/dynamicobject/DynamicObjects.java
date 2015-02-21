package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.NoSuchElementException;
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
    private static final Object EOF = Clojure.read(":eof");

    static String serialize(Object obj) {
        StringWriter stringWriter = new StringWriter();
        serialize(obj, stringWriter);
        return stringWriter.toString();
    }

    static void serialize(Object object,  Writer writer) {
        if (object instanceof DynamicObject)
            PrOn.invoke(((DynamicObject) object).getMap(), writer);
        else
            PrOn.invoke(object, writer);
        try {
          writer.flush();
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
    }

    static <T> T deserialize(String edn, Class<T> type) {
        return deserialize(new PushbackReader(new StringReader(edn)), type);
    }

    @SuppressWarnings("unchecked")
    static <T> T deserialize(PushbackReader streamReader, Class<T> type) {
        Object opts = getReadersAsOptions();
        opts = Assoc.invoke(opts, EOF, EOF);
        Object obj = Read.invoke(opts, streamReader);
        if (EOF.equals(obj))
            throw new NoSuchElementException();
        if (DynamicObject.class.isAssignableFrom(type))
            return wrap(obj, type);
        return (T) obj;
    }

    static <T> Stream<T> deserializeStream(PushbackReader streamReader, Class<T> type) {
        Iterator<T> iterator = deserializeStreamToIterator(streamReader, type);
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.IMMUTABLE);
        return StreamSupport.stream(spliterator, false);
    }

    private static <T> Iterator<T> deserializeStreamToIterator(PushbackReader streamReader, Class<T> type) {
        return new Iterator<T>() {
            private T stash = null;
            private boolean done = false;

            @Override
            public boolean hasNext() {
                populateStash();
                return !done || stash != null;
            }

            @Override
            public T next() {
                if (hasNext()) {
                    T ret = stash;
                    stash = null;
                    return ret;
                } else
                    throw new NoSuchElementException();
            }

            private void populateStash() {
                if (stash != null || done)
                    return;
                try {
                    stash = deserialize(streamReader, type);
                } catch (NoSuchElementException ignore) {
                    done = true;
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <T> T wrap(Object map, Class<T> type) {
        if (map == null)
            throw new NullPointerException("A null reference cannot be used as a DynamicObject");
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
