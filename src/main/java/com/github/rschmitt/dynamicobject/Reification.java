package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/*
 * This class contains functions responsible for taking collections, identifying unwrapped DynamicObject maps within
 * those collections, and wrapping them as proxies according to their :type metadata.
 */
class Reification {
    private static final IFn GET = Clojure.var("clojure.core", "get");
    private static final IFn META = Clojure.var("clojure.core", "meta");
    private static final IFn NAME = Clojure.var("clojure.core", "name");
    private static final IFn KEY = Clojure.var("clojure.core", "key");
    private static final IFn VAL = Clojure.var("clojure.core", "val");
    private static final IFn FIRST = Clojure.var("clojure.core", "first");
    private static final IFn REST = Clojure.var("clojure.core", "rest");

    @SuppressWarnings("unchecked")
    static Object wrapElements(Collection<Object> unwrappedSet, Collection<Object> ret) {
        unwrappedSet.stream().map(Reification::maybeWrapElement).forEach(ret::add);
        return ret;
    }

    static Map<Object, Object> wrapMapElements(Object unwrappedMap) {
        Map<Object, Object> ret = new HashMap<>();
        Object head = FIRST.invoke(unwrappedMap);
        unwrappedMap = REST.invoke(unwrappedMap);
        while (head != null) {
            Object key = KEY.invoke(head);
            Object val = VAL.invoke(head);
            key = maybeWrapElement(key);
            val = maybeWrapElement(val);
            ret.put(key, val);

            head = FIRST.invoke(unwrappedMap);
            unwrappedMap = REST.invoke(unwrappedMap);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    private static Object maybeWrapElement(Object obj) {
        Class<?> type = getTypeFromMetadata(obj);
        if (type == null)
            return obj;
        else
            return DynamicObject.wrap(obj, (Class<DynamicObject>) type);
    }

    private static Class<?> getTypeFromMetadata(Object obj) {
        String canonicalName = getTypeMetadata(obj);
        if (canonicalName == null) return null;
        try {
            return Class.forName(canonicalName);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String getTypeMetadata(Object obj) {
        Object metadata = META.invoke(obj);
        if (metadata == null) return null;
        Object typeMetadata = GET.invoke(metadata, Clojure.read(":type"));
        return (String) NAME.invoke(typeMetadata);
    }
}
