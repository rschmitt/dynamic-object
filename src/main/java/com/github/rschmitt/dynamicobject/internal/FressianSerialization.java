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

    public static FressianReader createFressianReader(InputStream is, boolean validateChecksum) {
        return new FressianReader(is, new MapLookup<>(fressianReadHandlers), validateChecksum);
    }

    public static FressianWriter createFressianWriter(OutputStream os) {
        return new FressianWriter(os, new InheritanceLookup<>(new MapLookup<>(fressianWriteHandlers)));
    }

    public static byte[] toFressianByteArray(Object o) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (FressianWriter fressianWriter = DynamicObject.createFressianWriter(baos)) {
            fressianWriter.writeObject(o);
            fressianWriter.close();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T> T fromFressianByteArray(byte[] bytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        FressianReader fressianReader = DynamicObject.createFressianReader(bais, false);
        try {
            return (T) fressianReader.readObject();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
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

    static synchronized <D extends DynamicObject<D>> void registerTag(Class<D> type, String tag) {
        binaryTagCache.put(type, tag);
        Handlers.installHandler(fressianWriteHandlers, type, tag, new FressianWriteHandler(type, tag, Reflection.cachedKeys(type)));
        fressianReadHandlers.putIfAbsent(tag, new FressianReadHandler(type));
    }

    static synchronized <D extends DynamicObject<D>> void deregisterTag(Class<D> type) {
        String tag = binaryTagCache.get(type);
        fressianWriteHandlers.remove(type);
        fressianReadHandlers.remove(tag);
    }
}
