package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.AFn;
import clojure.lang.BigInt;
import clojure.lang.Keyword;
import net.fushizen.invokedynamic.proxy.DynamicProxy;
import org.fressian.FressianReader;
import org.fressian.FressianWriter;
import org.fressian.handlers.ReadHandler;
import org.fressian.handlers.WriteHandler;
import org.fressian.impl.Handlers;
import org.fressian.impl.InheritanceLookup;
import org.fressian.impl.MapLookup;

import java.io.*;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.github.rschmitt.dynamicobject.ClojureStuff.*;
import static java.lang.String.format;

public class DynamicObjects {
    private static final AtomicReference<Object> translators = new AtomicReference<>(EmptyMap);
    private static final AtomicReference<AFn> defaultReader = new AtomicReference<>(getUnknownReader());
    private static final ConcurrentHashMap<Class<?>, String> recordTagCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, String> binaryTagCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, EdnTranslatorAdapter<?>> translatorCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class, Map<String, WriteHandler>> fressianWriteHandlers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Object, ReadHandler> fressianReadHandlers = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Class, DynamicProxy> proxyCache = new ConcurrentHashMap<>();
    private static final Object EOF = Clojure.read(":eof");
    private static final boolean USE_INVOKEDYNAMIC = true;

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

    private static AFn getUnknownReader() {
        String clojureCode = format(
                "(defmethod print-method %s [o, ^java.io.Writer w]" +
                        "(com.github.rschmitt.dynamicobject.Unknown/serialize o w))",
                Unknown.class.getTypeName());
        Eval.invoke(ReadString.invoke(clojureCode));
        return wrapReaderFunction(Unknown::new);
    }

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
        Object opts = getReadOptions();
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

        if (USE_INVOKEDYNAMIC)
            return createIndyProxy(map, type);
        else
            return createReflectionProxy(map, type);
    }

    private static <T> T createIndyProxy(Object map, Class<T> type) {
        try {
            T t = (T) proxyCache.computeIfAbsent(type, DynamicObjects::createProxy).constructor().invoke();
            DynamicObjectInstance i = (DynamicObjectInstance) t;
            i.map = map;
            i.type = type;
            return t;
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private static <T, U extends DynamicObject<U>> T createReflectionProxy(Object map, Class<T> type) {
        DynamicObjectInstance<U> instance = new DynamicObjectInstance<U>() {
            @Override
            public U $$customValidate() {
                return null;
            }
        };
        instance.type = (Class<U>) type;
        instance.map = map;
        return (T) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{type},
                new DynamicObjectInvocationHandler<>(instance)
        );
    }

    static DynamicProxy createProxy(Class dynamicObjectType) {
        try {
            return DynamicProxy.builder()
                    .withInterfaces(dynamicObjectType, CustomValidationHook.class)
                    .withSuperclass(DynamicObjectInstance.class)
                    .withInvocationHandler(new InvokeDynamicInvocationHandler(dynamicObjectType))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static <T extends DynamicObject<T>> T newInstance(Class<T> type) {
        return wrap(Metadata.withTypeMetadata(EmptyMap, type), type);
    }

    static synchronized <T> void registerType(Class<T> type, EdnTranslator<T> translator) {
        EdnTranslatorAdapter<T> adapter = new EdnTranslatorAdapter<>(translator);
        translatorCache.put(type, adapter);
        translators.getAndUpdate(translators -> Assoc.invoke(translators, cachedRead(translator.getTag()), adapter));
        definePrintMethod(type.getTypeName(), "DynamicObjects/invokeWriter", translator.getTag());
    }

    static synchronized void registerType(Class type, String tag, ReadHandler readHandler, WriteHandler writeHandler) {
        binaryTagCache.put(type, tag);
        Handlers.installHandler(fressianWriteHandlers, type, tag, writeHandler);
        fressianReadHandlers.putIfAbsent(tag, readHandler);
    }

    @SuppressWarnings("unchecked")
    static synchronized <T> void deregisterType(Class<T> type) {
        EdnTranslatorAdapter<T> adapter = (EdnTranslatorAdapter<T>) translatorCache.get(type);
        translators.getAndUpdate(translators -> Dissoc.invoke(translators, cachedRead(adapter.getTag())));
        RemoveMethod.invoke(PrintMethod, adapter);
        translatorCache.remove(type);

        fressianWriteHandlers.remove(type);
        String tag = binaryTagCache.get(type);
        if (tag != null) {
            fressianReadHandlers.remove(tag);
            binaryTagCache.remove(type);
        }
    }

    static synchronized <T extends DynamicObject<T>> void registerTag(Class<T> type, String tag) {
        recordTagCache.put(type, tag);
        translators.getAndUpdate(translators -> Assoc.invoke(translators, cachedRead(tag), new RecordReader<>(type)));
        definePrintMethod(":" + type.getTypeName(), "RecordPrinter/printRecord", tag);

        Handlers.installHandler(fressianWriteHandlers, type, tag, DynamicObject.getFressianWriteHandler(tag, type));
        fressianReadHandlers.putIfAbsent(tag, DynamicObject.getFressianReadHandler(tag, type));
    }

    static synchronized <T extends DynamicObject<T>> void deregisterTag(Class<T> type) {
        String tag = recordTagCache.get(type);
        translators.getAndUpdate(translators -> Dissoc.invoke(translators, cachedRead(tag)));
        recordTagCache.remove(type);

        Object dispatchVal = cachedRead(":" + type.getTypeName());
        RemoveMethod.invoke(PrintMethod, dispatchVal);

        fressianWriteHandlers.remove(type);
        fressianReadHandlers.remove(tag);
    }

    private static Object getReadOptions() {
        Object map = Assoc.invoke(EmptyMap, Readers, translators.get());
        AFn defaultReader = DynamicObjects.defaultReader.get();
        if (defaultReader != null) {
            map = Assoc.invoke(map, Default, defaultReader);
        }
        return map;
    }

    @SuppressWarnings("unused")
    public static Object invokeWriter(Object obj, Writer writer, String tag) {
        EdnTranslatorAdapter translator = (EdnTranslatorAdapter<?>) Get.invoke(translators.get(), cachedRead(tag));
        return translator.invoke(obj, writer);
    }

    private static void definePrintMethod(String dispatchVal, String method, String arg) {
        String clojureCode = format("(defmethod print-method %s [o, ^java.io.Writer w] (com.github.rschmitt.dynamicobject.%s o w \"%s\"))",
                dispatchVal, method, arg);
        Eval.invoke(ReadString.invoke(clojureCode));
    }

    static <T> void setDefaultReader(BiFunction<String, Object, T> reader) {
        if (reader == null) {
            defaultReader.set(null);
            return;
        }
        defaultReader.set(wrapReaderFunction(reader));
    }

    private static <T> AFn wrapReaderFunction(BiFunction<String, Object, T> reader) {
        return new AFn() {
            @Override
            public Object invoke(Object arg1, Object arg2) {
                return reader.apply(arg1.toString(), arg2);
            }
        };
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
}
