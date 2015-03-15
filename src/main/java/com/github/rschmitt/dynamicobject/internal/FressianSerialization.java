package com.github.rschmitt.dynamicobject.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import com.github.rschmitt.dynamicobject.DynamicObject;
import com.github.rschmitt.dynamicobject.FressianReadHandler;
import com.github.rschmitt.dynamicobject.FressianWriteHandler;

public class FressianSerialization {
    private static final ConcurrentHashMap<Class, Map<String, WriteHandler>> fressianWriteHandlers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Object, ReadHandler> fressianReadHandlers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, String> binaryTagCache = new ConcurrentHashMap<>();

    static {
        fressianWriteHandlers.putAll(ClojureStuff.clojureWriteHandlers);
        fressianReadHandlers.putAll(ClojureStuff.clojureReadHandlers);
    }

    public static <T> Stream<T> deserializeFressianStream(InputStream is, Class<T> type) {
        FressianReader fressianReader = new FressianReader(is, new MapLookup<>(fressianReadHandlers));
        Iterator<T> iterator = Serialization.deserializeStreamToIterator(() -> (T) fressianReader.readObject(), type);
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.IMMUTABLE);
        return StreamSupport.stream(spliterator, false);
    }

    public static void serializeToFressian(Object o, OutputStream os) {
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
    public static <T> T deserializeFromFressian(InputStream is) {
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

    public static synchronized void registerType(Class type, String tag, ReadHandler readHandler, WriteHandler writeHandler) {
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
