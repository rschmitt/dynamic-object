package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

/*
 * This class contains functions responsible for taking collections, identifying unwrapped DynamicObject maps within
 * those collections, and wrapping them as proxies according to their :type metadata.
 */
class Reification {
    private static final Object TYPE = Clojure.read(":type");
    private static final IFn GET = Clojure.var("clojure.core", "get");
    private static final IFn META = Clojure.var("clojure.core", "meta");
    private static final IFn NAME = Clojure.var("clojure.core", "name");
    private static final IFn KEY = Clojure.var("clojure.core", "key");
    private static final IFn VAL = Clojure.var("clojure.core", "val");
    private static final IFn FIRST = Clojure.var("clojure.core", "first");
    private static final IFn REST = Clojure.var("clojure.core", "rest");
    private static final IFn TRANSIENT = Clojure.var("clojure.core", "transient");
    private static final IFn PERSISTENT = Clojure.var("clojure.core", "persistent!");
    private static final IFn ASSOC_BANG = Clojure.var("clojure.core", "assoc!");
    private static final IFn CONJ_BANG = Clojure.var("clojure.core", "conj!");

    @SuppressWarnings("unchecked")
    static Object wrapElements(Object coll, Object empty) {
        Object ret = TRANSIENT.invoke(empty);
        Object head = FIRST.invoke(coll);
        coll = REST.invoke(coll);
        while (head != null) {
            CONJ_BANG.invoke(ret, maybeWrapElement(head));
            head = FIRST.invoke(coll);
            coll = REST.invoke(coll);
        }
        return PERSISTENT.invoke(ret);
    }

    static Object wrapMapElements(Object unwrappedMap) {
        Object ret = Clojure.read("{}");
        ret = TRANSIENT.invoke(ret);
        Object head = FIRST.invoke(unwrappedMap);
        unwrappedMap = REST.invoke(unwrappedMap);
        while (head != null) {
            Object key = KEY.invoke(head);
            Object val = VAL.invoke(head);
            key = maybeWrapElement(key);
            val = maybeWrapElement(val);
            ASSOC_BANG.invoke(ret, key, val);

            head = FIRST.invoke(unwrappedMap);
            unwrappedMap = REST.invoke(unwrappedMap);
        }
        return PERSISTENT.invoke(ret);
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
        Object typeMetadata = GET.invoke(metadata, TYPE);
        return (String) NAME.invoke(typeMetadata);
    }
}
