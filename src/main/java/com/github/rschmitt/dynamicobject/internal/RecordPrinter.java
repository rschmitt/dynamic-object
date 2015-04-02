package com.github.rschmitt.dynamicobject.internal;

import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.PrOn;
import static java.lang.String.format;

import java.io.IOException;
import java.io.Writer;

import com.github.rschmitt.dynamicobject.DynamicObject;

public final class RecordPrinter {
    /**
     * For use by print-method only. Do not call directly.
     */
    public static Object printRecord(Object obj, Writer writer, String tag) throws IOException {
        writer.write(format("#%s", tag));
        if (obj instanceof DynamicObject)
            obj = ((DynamicObject) obj).getMap();
        obj = ClojureStuff.WithMeta.invoke(obj, null);
        PrOn.invoke(obj, writer);
        return null;
    }
}
