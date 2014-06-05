package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.AFn;
import clojure.lang.IFn;

public final class RecordReader<T extends DynamicObject<T>> extends AFn {
    private static final Object EMPTY_MAP = Clojure.read("{}");
    private static final IFn META = Clojure.var("clojure.core", "meta");
    private static final IFn ASSOC = Clojure.var("clojure.core", "assoc");
    private static final IFn WITH_META = Clojure.var("clojure.core", "with-meta");

    private final Class<T> type;

    RecordReader(Class<T> type) {
        this.type = type;
    }

    /**
     * For use by clojure.edn/read only. Do not call directly.
     */
    @Override
    public Object invoke(Object mapWithMeta) {
        Object meta = META.invoke(mapWithMeta);
        if (meta == null)
            meta = EMPTY_MAP;
        Object newMeta = ASSOC.invoke(meta, Clojure.read(":type"),
                Clojure.read(":" + type.getCanonicalName()));
        return WITH_META.invoke(mapWithMeta, newMeta);
    }
}
