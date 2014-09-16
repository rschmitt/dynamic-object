package com.github.rschmitt.dynamicobject;

public interface EdnTranslator<T> {
    /**
     * Read a tagged Edn object as its intended type.
     */
    T read(Object obj);

    /**
     * Return an Edn representation of the given object.
     */
    String write(T obj);

    /**
     * Return the tag literal to use during serialization.
     */
    String getTag();
}
