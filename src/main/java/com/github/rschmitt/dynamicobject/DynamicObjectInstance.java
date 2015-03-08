package com.github.rschmitt.dynamicobject;

import clojure.lang.AFn;

import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.rschmitt.dynamicobject.ClojureStuff.*;

public class DynamicObjectInstance<T extends DynamicObject<T>> {
    private volatile Object map;
    private volatile Class<T> type;
    private final ConcurrentHashMap valueCache = new ConcurrentHashMap();

    public DynamicObjectInstance(Object map, Class<T> type) {
        this.map = map;
        this.type = type;
    }

    public Object getMap() {
        return map;
    }

    public void setMap(Object map) {
        this.map = map;
    }

    public Class<T> getType() {
        return type;
    }

    public void setType(Class<T> type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return map.toString();
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    public void prettyPrint() {
        Pprint.invoke(map);
    }

    public String toFormattedString() {
        Writer w = new StringWriter();
        Pprint.invoke(map, w);
        return w.toString();
    }

    public T merge(T other) {
        AFn ignoreNulls = new AFn() {
            public Object invoke(Object arg1, Object arg2) {
                return (arg2 == null) ? arg1 : arg2;
            }
        };
        Object mergedMap = MergeWith.invoke(ignoreNulls, map, other.getMap());
        return DynamicObject.wrap(mergedMap, type);
    }

    public T intersect(T arg) {
        return diff(arg, 2);
    }

    public T subtract(T arg) {
        return diff(arg, 0);
    }

    private T diff(T arg, int idx) {
        Object array = Diff.invoke(map, arg.getMap());
        Object union = Nth.invoke(array, idx);
        if (union == null) union = EmptyMap;
        union = Metadata.withTypeMetadata(union, type);
        return DynamicObject.wrap(union, type);
    }

    public T assoc(Object key, Object value) {
        if (value instanceof DynamicObject)
            value = ((DynamicObject) value).getMap();
        return DynamicObject.wrap(Assoc.invoke(map, key, value), type);
    }

    public Object invokeGetter(Object key) {
        return Get.invoke(map, key);
    }
}
