package com.github.rschmitt.dynamicobject.internal;

import java.util.Map;

import com.github.rschmitt.dynamicobject.DynamicObject;

import clojure.lang.AFn;

public final class RecordReader<D extends DynamicObject<D>> extends AFn {
    private final Class<D> type;

    RecordReader(Class<D> type) {
        this.type = type;
    }

    /**
     * For use by clojure.edn/read only. Do not call directly.
     */
    @Override
    public Object invoke(Object map) {
        Object mapWithMeta = Metadata.withTypeMetadata(map, type);
        return DynamicObject.wrap((Map) mapWithMeta, type);
    }
}
