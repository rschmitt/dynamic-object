package com.github.rschmitt.dynamicobject;

import java.util.Map;

import static com.github.rschmitt.dynamicobject.ClojureStuff.*;

/*
 * This class contains functions responsible for taking proxied DynamicObjects (or collections of same) and replacing
 * them with their underlying Clojure maps. Said maps will have :type metadata attached, indicating the specific
 * DynamicObject class that the map is an instance of.
 */
class Erasure {
    static Object unwrapCollectionElements(Object val, Class<?> type, Object empty) {
        if (val != null && type.isAssignableFrom(val.getClass())) {
            Iterable<?> iterable = (Iterable<?>) val;
            Object ret = empty;
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
        Object newMeta = ASSOC.invoke(meta, TYPE, cachedRead(":" + type.getCanonicalName()));
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
