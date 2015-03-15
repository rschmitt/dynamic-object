package com.github.rschmitt.dynamicobject.internal;

import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.PrOn;
import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.WithMeta;
import static java.lang.String.format;

import java.io.IOException;
import java.io.Writer;

public final class RecordPrinter {
    /**
     * For use by print-method only. Do not call directly.
     */
    public static Object printRecord(Object obj, Writer writer, String tag) throws IOException {
        obj = WithMeta.invoke(obj, null);
        writer.write(format("#%s", tag));
        PrOn.invoke(obj, writer);
        return null;
    }
}
