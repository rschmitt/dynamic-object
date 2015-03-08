package com.github.rschmitt.dynamicobject;

import clojure.lang.AFn;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.rschmitt.dynamicobject.ClojureStuff.*;
import static java.lang.String.format;

class DynamicObjectInstance<T extends DynamicObject<T>> {
    private static final Object Default = new Object();
    private static final Object Null = new Object();

    private volatile Object map;
    private volatile Class<T> type;
    private final ConcurrentHashMap valueCache = new ConcurrentHashMap();

    DynamicObjectInstance(Object map, Class<T> type) {
        this.map = map;
        this.type = type;
    }

    public Map getMap() {
        return (Map) map;
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

    public boolean equals(Object other) {
        if (other instanceof DynamicObject)
            return map.equals(((DynamicObject) other).getMap());
        else
            return other.equals(map);
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

    public T assocMeta(Object key, Object value) {
        return DynamicObject.wrap(VaryMeta.invoke(map, Assoc, key, value), type);
    }

    public Object getMetadataFor(Object key) {
        Object meta = Meta.invoke(map);
        return Get.invoke(meta, key);
    }

    public Object invokeGetter(Object key, boolean isRequired, Type genericReturnType) {
        Object value = getAndCacheValueFor(key, genericReturnType);
        if (value == null && isRequired)
            throw new NullPointerException(format("Required field %s was null", key.toString()));
        return value;
    }

    @SuppressWarnings("unchecked")
    public Object getAndCacheValueFor(Object key, Type genericReturnType) {
        Object cachedValue = valueCache.getOrDefault(key, Default);
        if (cachedValue == Null) return null;
        if (cachedValue != Default) return cachedValue;
        Object value = getValueFor(key, genericReturnType);
        if (value == null)
            valueCache.putIfAbsent(key, Null);
        else
            valueCache.putIfAbsent(key, value);
        return value;
    }

    public Object getValueFor(Object key, Type genericReturnType) {
        Object val = Get.invoke(map, key);
        return Conversions.clojureToJava(val, genericReturnType);
    }

    public T validate(T self) {
        Validation.validateInstance(this);
        return self;
    }
}
