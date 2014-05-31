package com.github.rschmitt.dynamicobject;

import clojure.lang.AFn;

import java.io.IOException;
import java.io.Writer;

public abstract class EdnTranslator<T> extends AFn {
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

    /**
     * For use by EdnReader only. Do not call directly.
     */
    @Override
    public final Object invoke(Object arg) {
        return read(arg);
    }

    /**
     * For use by print-method only. Do not call directly.
     */
    @Override
    @SuppressWarnings("unchecked")
    public final Object invoke(Object arg1, Object arg2) {
        Writer writer = (Writer) arg2;
        try {
            String output = String.format("#%s%s", getTag(), write((T) arg1));
            writer.write(output);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return null;
    }
}
