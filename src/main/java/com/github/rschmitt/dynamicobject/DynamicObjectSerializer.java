package com.github.rschmitt.dynamicobject;

import com.github.rschmitt.dynamicobject.internal.EdnSerialization;
import com.github.rschmitt.dynamicobject.internal.FressianSerialization;

import org.fressian.FressianReader;
import org.fressian.FressianWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackReader;
import java.io.Writer;
import java.util.stream.Stream;

/**
 * A utility class for DynamicObject (de)serialization. All of the methods in this class delegate
 * directly to the static methods in {@linkplain DynamicObject}. The difference is that this class
 * is instantiable, and can therefore participate in dependency injection. This makes it
 * straightforward to ensure that types and serialization tags are registered with DynamicObject
 * before any serialization is attempted.
 * <p/>
 * For example, if you are using <a href="https://github.com/google/guice">Guice</a>, you can write
 * a {@code DynamicObjectSerializer} provider method that registers types: <blockquote><pre>
 * &#064;Provides
 * &#064;Singleton
 * DynamicObjectSerializer getDynamicObjectSerializer() {
 *     DynamicObject.registerTag(Record.class, "recordtag");
 *     DynamicObject.registerType(Identifier.class, new IdentifierTranslator());
 *     return new DynamicObjectSerializer();
 * }
 * </pre></blockquote>
 * Classes that need to perform serialization can then have a {@code DynamicObjectSerializer}
 * injected at construction time:<blockquote><pre>
 * private final DynamicObjectSerializer serializer;
 *
 * &#064;Inject
 * public FlatFileWriter(DynamicObjectSerializer serializer) {
 *     this.serializer = serializer;
 * }
 *
 * public void persist(Record rec) throws IOException {
 *     File file = new File("record.txt");
 *     try (
 *         OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
 *         Writer w = new OutputStreamWriter(os, StandardCharsets.UTF_8)
 *     ) {
 *         serializer.serialize(rec, w);
 *     }
 * }
 * </pre></blockquote>
 */
public class DynamicObjectSerializer {
    /**
     * @see DynamicObject#serialize(Object)
     */
    public String serialize(Object o) {
        return EdnSerialization.serialize(o);
    }

    /**
     * @see DynamicObject#serialize(Object, Writer)
     */
    public void serialize(Object o, Writer w) {
        EdnSerialization.serialize(o, w);
    }

    /**
     * @see DynamicObject#deserialize(String, Class)
     */
    public <T> T deserialize(String edn, Class<T> type) {
        return EdnSerialization.deserialize(edn, type);
    }

    /**
     * @see DynamicObject#deserializeStream(PushbackReader, Class)
     */
    public <T> Stream<T> deserializeStream(PushbackReader streamReader, Class<T> type) {
        return EdnSerialization.deserializeStream(streamReader, type);
    }

    /**
     * @see DynamicObject#toFressianByteArray(Object)
     */
    public byte[] toFressianByteArray(Object o) {
        return FressianSerialization.toFressianByteArray(o);
    }

    /**
     * @see DynamicObject#fromFressianByteArray(byte[])
     */
    public <T> T fromFressianByteArray(byte[] bytes) {
        return FressianSerialization.fromFressianByteArray(bytes);
    }

    /**
     * @see DynamicObject#createFressianReader(InputStream, boolean)
     */
    public FressianReader createFressianReader(InputStream is, boolean validateChecksum) {
        return FressianSerialization.createFressianReader(is, validateChecksum);
    }

    /**
     * @see DynamicObject#createFressianWriter(OutputStream)
     */
    public FressianWriter createFressianWriter(OutputStream os) {
        return FressianSerialization.createFressianWriter(os);
    }

    /**
     * @see DynamicObject#deserializeFressianStream(InputStream, Class)
     */
    public <T> Stream<T> deserializeFressianStream(InputStream is, Class<T> type) {
        return FressianSerialization.deserializeFressianStream(is, type);
    }
}
