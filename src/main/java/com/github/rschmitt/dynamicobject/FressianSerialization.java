package com.github.rschmitt.dynamicobject;

import static com.github.rschmitt.dynamicobject.ClojureStuff.Bigint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.fressian.FressianReader;
import org.fressian.FressianWriter;
import org.fressian.handlers.ReadHandler;
import org.fressian.handlers.WriteHandler;
import org.fressian.impl.Handlers;
import org.fressian.impl.InheritanceLookup;
import org.fressian.impl.MapLookup;

import clojure.lang.BigInt;
import clojure.lang.Keyword;

public class FressianSerialization {
    private static final ConcurrentHashMap<Class, Map<String, WriteHandler>> fressianWriteHandlers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Object, ReadHandler> fressianReadHandlers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, String> binaryTagCache = new ConcurrentHashMap<>();

    static {
        Handlers.installHandler(fressianWriteHandlers, Keyword.class, "key", (w, instance) -> {
            Keyword keyword = (Keyword) instance;
            w.writeTag("key", 2);
            w.writeObject(keyword.getNamespace(), true);
            w.writeObject(keyword.getName(), true);
        });

        fressianReadHandlers.put("key", (r, tag, componentCount) -> {
            String ns = (String) r.readObject();
            String name = (String) r.readObject();
            return Keyword.intern(ns, name);
        });

        Handlers.installHandler(fressianWriteHandlers, BigInt.class, "bigint", (w, instance) -> {
            w.writeTag("bigint", 1);
            w.writeBytes(((BigInt) instance).toBigInteger().toByteArray());
        });

        fressianReadHandlers.put("bigint", (r, tag, componentCount) -> {
            BigInteger bigInteger = new BigInteger((byte[]) r.readObject());
            return Bigint.invoke(bigInteger);
        });
    }

    static <T> Stream<T> deserializeFressianStream(InputStream is, Class<T> type) {
        FressianReader fressianReader = new FressianReader(is, new MapLookup<>(fressianReadHandlers));
        Iterator<T> iterator = Serialization.deserializeStreamToIterator(() -> (T) fressianReader.readObject(), type);
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.IMMUTABLE);
        return StreamSupport.stream(spliterator, false);
    }

    static void serializeToFressian(Object o, OutputStream os) {
        FressianWriter fressianWriter = new FressianWriter(os, new InheritanceLookup<>(new MapLookup<>(fressianWriteHandlers)));
        try {
            fressianWriter.writeObject(o);
            fressianWriter.writeFooter();
            fressianWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T deserializeFromFressian(InputStream is) {
        FressianReader fressianReader = new FressianReader(is, new MapLookup<>(fressianReadHandlers));
        try {
            Object o = fressianReader.readObject();
            fressianReader.validateFooter();
            fressianReader.close();
            return (T) o;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static byte[] toFressianByteArray(Object o) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DynamicObject.serializeToFressian(o, baos);
        return baos.toByteArray();
    }

    public static <T> T fromFressianByteArray(byte[] bytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return DynamicObject.deserializeFromFressian(bais);
    }

    static synchronized void registerType(Class type, String tag, ReadHandler readHandler, WriteHandler writeHandler) {
        binaryTagCache.put(type, tag);
        Handlers.installHandler(fressianWriteHandlers, type, tag, writeHandler);
        fressianReadHandlers.putIfAbsent(tag, readHandler);
    }

    static synchronized <T> void deregisterType(Class<T> type) {
        fressianWriteHandlers.remove(type);
        String tag = binaryTagCache.get(type);
        if (tag != null) {
            fressianReadHandlers.remove(tag);
            binaryTagCache.remove(type);
        }
    }

    static synchronized <T extends DynamicObject<T>> void registerTag(Class<T> type, String tag) {
        binaryTagCache.put(type, tag);
        Handlers.installHandler(fressianWriteHandlers, type, tag, new FressianWriteHandler(type, tag));
        fressianReadHandlers.putIfAbsent(tag, new FressianReadHandler(type));
    }

    static synchronized <T extends DynamicObject<T>> void deregisterTag(Class<T> type) {
        String tag = binaryTagCache.get(type);
        fressianWriteHandlers.remove(type);
        fressianReadHandlers.remove(tag);
    }
}
