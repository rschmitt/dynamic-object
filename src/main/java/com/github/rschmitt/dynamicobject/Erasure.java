package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import java.util.Map;

/*
 * This class contains functions responsible for taking proxied DynamicObjects (or collections of same) and replacing
 * them with their underlying Clojure maps. Said maps will have :type metadata attached, indicating the specific
 * DynamicObject class that the map is an instance of.
 */
class Erasure {
    private static final Object EMPTY_MAP = Clojure.read("{}");
    private static final Object TYPE = Clojure.read(":type");
    private static final IFn ASSOC = Clojure.var("clojure.core", "assoc");
    private static final IFn META = Clojure.var("clojure.core", "meta");
    private static final IFn WITH_META = Clojure.var("clojure.core", "with-meta");

    private static final IFn TRANSIENT = Clojure.var("clojure.core", "transient");
    private static final IFn PERSISTENT = Clojure.var("clojure.core", "persistent!");
    private static final IFn ASSOC_BANG = Clojure.var("clojure.core", "assoc!");
    private static final IFn CONJ_BANG = Clojure.var("clojure.core", "conj!");

    static Object unwrapCollectionElements(Object val, Class<?> type, String empty) {
        if (val != null && type.isAssignableFrom(val.getClass())) {
            Iterable<?> iterable = (Iterable<?>) val;
            Object ret = Clojure.read(empty);
            ret = TRANSIENT.invoke(ret);
            for (Object o : iterable)
                CONJ_BANG.invoke(ret, unwrapAndAnnotate(o));
            return PERSISTENT.invoke(ret);
        }
        return val;
    }

    private static Object unwrapAndAnnotate(Object o) {
        if (o instanceof DynamicObject) {
            DynamicObject<?> dynamicObject = (DynamicObject<?>) o;
            Object map = dynamicObject.getMap();
            map = withTypeMetadata(map, dynamicObject.getType());
            return map;
        }
        return o;
    }

    static Object withTypeMetadata(Object obj, Class<?> type) {
        Object meta = META.invoke(obj);
        if (meta == null) meta = EMPTY_MAP;
        Object newMeta = ASSOC.invoke(meta, TYPE, Clojure.read(":" + type.getCanonicalName()));
        return WITH_META.invoke(obj, newMeta);
    }

    @SuppressWarnings("unchecked")
    static Object unwrapMapElements(Object obj) {
        if (obj != null && Map.class.isAssignableFrom(obj.getClass())) {
            Map<Object, Object> map = (Map<Object, Object>) obj;
            Object ret = TRANSIENT.invoke(EMPTY_MAP);
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                Object key = entry.getKey();
                Object val = entry.getValue();
                key = unwrapAndAnnotate(key);
                val = unwrapAndAnnotate(val);
                ASSOC_BANG.invoke(ret, key, val);
            }
            return PERSISTENT.invoke(ret);
        }
        return obj;
    }
}
