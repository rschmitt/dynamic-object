package com.github.rschmitt.dynamicobject;

public interface EdnTranslator<T> {
    /**
     * Read a tagged Edn object as its intended type.
     */
    public abstract T read(Object obj);

    /**
     * Return an Edn representation of the given object.
     */
    public abstract String write(T obj);

    /**
     * Return the tag literal to use during serialization.
     */
    public abstract String getTag();
}
