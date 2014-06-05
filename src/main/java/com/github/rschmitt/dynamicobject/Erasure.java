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
    private static final IFn ASSOC = Clojure.var("clojure.core", "assoc");
    private static final IFn CONJ = Clojure.var("clojure.core", "conj");
    private static final IFn META = Clojure.var("clojure.core", "meta");
    private static final IFn WITH_META = Clojure.var("clojure.core", "with-meta");

    static Object unwrapCollectionElements(Object val, Class<?> type, String empty) {
        if (val != null && type.isAssignableFrom(val.getClass())) {
            Iterable<?> iterable = (Iterable<?>) val;
            Object ret = Clojure.read(empty);
            for (Object o : iterable)
                ret = CONJ.invoke(ret, unwrapAndAnnotate(o));
            return ret;
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
        Object newMeta = ASSOC.invoke(meta, Clojure.read(":type"), Clojure.read(":" + type.getCanonicalName()));
        return WITH_META.invoke(obj, newMeta);
    }

    @SuppressWarnings("unchecked")
    static Object unwrapMapElements(Object obj) {
        if (obj != null && Map.class.isAssignableFrom(obj.getClass())) {
            Map<Object, Object> map = (Map<Object, Object>) obj;
            Object ret = EMPTY_MAP;
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                Object key = entry.getKey();
                Object val = entry.getValue();
                key = unwrapAndAnnotate(key);
                val = unwrapAndAnnotate(val);
                ret = ASSOC.invoke(ret, key, val);
            }
            return ret;
        }
        return obj;
    }
}
