package com.github.rschmitt.dynamicobject;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public interface EdnTranslator<T> {
    /**
     * Read a tagged Edn object as its intended type.
     */
    T read(Object obj);

    /**
     * Return an Edn representation of the given object.
     */
    default String write(T obj) {
        StringWriter stringWriter = new StringWriter();
        write(obj, stringWriter);
        return stringWriter.toString();
    }

    /**
     * Return the tag literal to use during serialization.
     */
    String getTag();

    /**
     * Write an Edn representation of the given object to the given Writer.
     */
    default void write(T obj, Writer writer) {
        try {
            writer.write(write(obj));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
