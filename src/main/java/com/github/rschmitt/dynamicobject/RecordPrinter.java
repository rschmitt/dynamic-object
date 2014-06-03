package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import java.io.IOException;
import java.io.Writer;

import static java.lang.String.format;

public class RecordPrinter {
    private static final IFn WITH_META = Clojure.var("clojure.core", "with-meta");

    /**
     * For use by print-method only. Do not call directly.
     */
    public static Object printRecord(Object obj, Writer writer, String tag) {
        obj = WITH_META.invoke(obj, null);
        try {
            writer.write(format("#%s%s", tag, obj.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
