package com.github.rschmitt.dynamicobject;

import java.io.IOException;
import java.io.Writer;

import static com.github.rschmitt.dynamicobject.ClojureStuff.WithMeta;
import static java.lang.String.format;

public final class RecordPrinter {
    /**
     * For use by print-method only. Do not call directly.
     */
    public static Object printRecord(Object obj, Writer writer, String tag) throws IOException {
        obj = WithMeta.invoke(obj, null);
        writer.write(format("#%s%s", tag, obj.toString()));
        return null;
    }
}
