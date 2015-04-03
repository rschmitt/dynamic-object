package com.github.rschmitt.dynamicobject.internal;

import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.Eval;
import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.PreferMethod;
import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.PrintMethod;
import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.ReadString;
import static java.lang.String.format;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.rschmitt.dynamicobject.DynamicObject;
import com.github.rschmitt.dynamicobject.EdnTranslator;
import com.github.rschmitt.dynamicobject.Unknown;

import clojure.java.api.Clojure;
import clojure.lang.AFn;
import clojure.lang.IPersistentMap;

public class EdnSerialization {
    static {
        String clojureCode =
        "(defmethod print-method com.github.rschmitt.dynamicobject.internal.DynamicObjectPrintHook " +
                "[o, ^java.io.Writer w]" +
                "(com.github.rschmitt.dynamicobject.internal.EdnSerialization/invokePrintMethod o w))";
        Eval.invoke(ReadString.invoke(clojureCode));
        PreferMethod.invoke(PrintMethod, DynamicObjectPrintHook.class, IPersistentMap.class);
        PreferMethod.invoke(PrintMethod, DynamicObjectPrintHook.class, Map.class);
    }

    public static class DynamicObjectPrintMethod extends AFn {
        @Override
        public Object invoke(Object arg1, Object arg2) {
            DynamicObject dynamicObject = (DynamicObject) arg1;
            Writer writer = (Writer) arg2;
            String tag = recordTagCache.getOrDefault(dynamicObject.getType(), null);
            try {
                if (tag != null) {
                    writer.write("#");
                    writer.write(tag);
                }
                ClojureStuff.PrOn.invoke(dynamicObject.getMap(), writer);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            return null;
        }
    }

    private static final DynamicObjectPrintMethod dynamicObjectPrintMethod = new DynamicObjectPrintMethod();
    private static final AtomicReference<Object> translators = new AtomicReference<>(ClojureStuff.EmptyMap);
    private static final ConcurrentHashMap<Class<?>, EdnTranslatorAdapter<?>> translatorCache = new ConcurrentHashMap<>();
    private static final AtomicReference<AFn> defaultReader = new AtomicReference<>(getUnknownReader());
    private static final ConcurrentHashMap<Class<?>, String> recordTagCache = new ConcurrentHashMap<>();
    private static final Object EOF = Clojure.read(":eof");

    public static String serialize(Object obj) {
        StringWriter stringWriter = new StringWriter();
        serialize(obj, stringWriter);
        return stringWriter.toString();
    }

    public static void serialize(Object object,  Writer writer) {
        ClojureStuff.PrOn.invoke(object, writer);
        try {
            writer.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T> T deserialize(String edn, Class<T> type) {
        return deserialize(new PushbackReader(new StringReader(edn)), type);
    }

    @SuppressWarnings("unchecked")
    static <T> T deserialize(PushbackReader streamReader, Class<T> type) {
        Object opts = getReadOptions();
        opts = ClojureStuff.Assoc.invoke(opts, EOF, EOF);
        Object obj = ClojureStuff.Read.invoke(opts, streamReader);
        if (EOF.equals(obj))
            throw new NoSuchElementException();
        if (DynamicObject.class.isAssignableFrom(type) && !(obj instanceof DynamicObject)) {
            return Instances.wrap((Map) obj, type);
        }
        return type.cast(obj);
    }

    public static <T> Stream<T> deserializeStream(PushbackReader streamReader, Class<T> type) {
        Iterator<T> iterator = Serialization.deserializeStreamToIterator(() -> deserialize(streamReader, type), type);
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.IMMUTABLE);
        return StreamSupport.stream(spliterator, false);
    }

    private static AFn getUnknownReader() {
        String clojureCode = format("(defmethod print-method %s [o, ^java.io.Writer w]" +
                "(com.github.rschmitt.dynamicobject.Unknown/serialize o w))", Unknown.class.getTypeName());
        ClojureStuff.Eval.invoke(ClojureStuff.ReadString.invoke(clojureCode));
        return wrapReaderFunction(Unknown::new);
    }

    public static <T> void setDefaultReader(BiFunction<String, Object, T> reader) {
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

    private static Object getReadOptions() {
        Object map = ClojureStuff.Assoc.invoke(ClojureStuff.EmptyMap, ClojureStuff.Readers, translators.get());
        AFn defaultReader = EdnSerialization.defaultReader.get();
        if (defaultReader != null) {
            map = ClojureStuff.Assoc.invoke(map, ClojureStuff.Default, defaultReader);
        }
        return map;
    }

    public static synchronized <T> void registerType(Class<T> type, EdnTranslator<T> translator) {
        EdnTranslatorAdapter<T> adapter = new EdnTranslatorAdapter<>(translator);
        translatorCache.put(type, adapter);
        translators.getAndUpdate(translators -> ClojureStuff.Assoc.invoke(translators, ClojureStuff.cachedRead(
                translator.getTag()), adapter));
        String clojureCode = format(
                "(defmethod print-method %s " +
                        "[o, ^java.io.Writer w] " +
                        "(com.github.rschmitt.dynamicobject.internal.EdnSerialization/invokeWriter o w \"%s\"))",
                type.getTypeName(), translator.getTag());
        ClojureStuff.Eval.invoke(ClojureStuff.ReadString.invoke(clojureCode));
    }

    public static synchronized <T> void deregisterType(Class<T> type) {
        EdnTranslatorAdapter<T> adapter = (EdnTranslatorAdapter<T>) translatorCache.get(type);
        translators.getAndUpdate(translators -> ClojureStuff.Dissoc.invoke(translators, ClojureStuff.cachedRead(
                adapter.getTag())));
        ClojureStuff.RemoveMethod.invoke(PrintMethod, adapter);
        translatorCache.remove(type);
    }

    public static synchronized <D extends DynamicObject<D>> void registerTag(Class<D> type, String tag) {
        recordTagCache.put(type, tag);
        translators.getAndUpdate(translators -> ClojureStuff.Assoc.invoke(translators, ClojureStuff.cachedRead(
                tag), new RecordReader<>(type)));
    }

    public static synchronized <D extends DynamicObject<D>> void deregisterTag(Class<D> type) {
        String tag = recordTagCache.get(type);
        translators.getAndUpdate(translators -> ClojureStuff.Dissoc.invoke(translators, ClojureStuff.cachedRead(tag)));
        recordTagCache.remove(type);
    }

    @SuppressWarnings("unused")
    public static Object invokeWriter(Object obj, Writer writer, String tag) {
        EdnTranslatorAdapter translator = (EdnTranslatorAdapter<?>) ClojureStuff.Get.invoke(translators.get(), ClojureStuff
                .cachedRead(tag));
        return translator.invoke(obj, writer);
    }

    public static Object invokePrintMethod(Object arg1, Object arg2) {
        return dynamicObjectPrintMethod.invoke(arg1, arg2);
    }
}
