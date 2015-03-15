package com.github.rschmitt.dynamicobject.internal;

import com.github.rschmitt.dynamicobject.DynamicObject;
import clojure.lang.AFn;

public final class RecordReader<T extends DynamicObject<T>> extends AFn {
    private final Class<T> type;

    RecordReader(Class<T> type) {
        this.type = type;
    }

    /**
     * For use by clojure.edn/read only. Do not call directly.
     */
    @Override
    public Object invoke(Object mapWithMeta) {
        return Metadata.withTypeMetadata(mapWithMeta, type);
    }
}
