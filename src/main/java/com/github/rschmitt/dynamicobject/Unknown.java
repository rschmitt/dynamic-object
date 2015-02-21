package com.github.rschmitt.dynamicobject;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

/**
 * A generic container for tagged Edn elements. This class preserves everything the Edn reader sees when an unknown
 * reader tag is encountered.
 */
public class Unknown {
    private final String tag;
    private final Object element;

    /**
     * For internal use only. Serialize a tagged element of an unknown type.
     */
    @SuppressWarnings("unused")
    public static Object serialize(Unknown unknown, Writer w) throws IOException {
        w.append('#');
        w.append(unknown.getTag());
        if (!(unknown.getElement() instanceof Map))
            w.append(' ');
        ClojureStuff.PrOn.invoke(unknown.getElement(), w);
        return null;
    }

    public Unknown(String tag, Object element) {
        this.tag = tag;
        this.element = element;
    }

    public String getTag() {
        return tag;
    }

    public Object getElement() {
        return element;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Unknown unknown = (Unknown) o;

        if (!element.equals(unknown.element)) return false;
        if (!tag.equals(unknown.tag)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = tag.hashCode();
        result = 31 * result + element.hashCode();
        return result;
    }

    @Override
    public String toString() {
        try {
            StringWriter stringWriter = new StringWriter();
            serialize(this, stringWriter);
            return stringWriter.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
