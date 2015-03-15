package com.github.rschmitt.dynamicobject.internal;

import static java.lang.String.format;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.rschmitt.dynamicobject.DynamicObject;
import clojure.lang.AFn;

public abstract class DynamicObjectInstance<D extends DynamicObject<D>> implements CustomValidationHook<D> {
    private static final Object Default = new Object();
    private static final Object Null = new Object();

    Object map;
    Class<D> type;
    private final ConcurrentHashMap valueCache = new ConcurrentHashMap();

    public DynamicObjectInstance() {
    }

    DynamicObjectInstance(Object map, Class<D> type) {
        this.map = map;
        this.type = type;
    }

    public Map getMap() {
        return (Map) map;
    }

    public Class<D> getType() {
        return type;
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
        ClojureStuff.Pprint.invoke(map);
    }

    public String toFormattedString() {
        Writer w = new StringWriter();
        ClojureStuff.Pprint.invoke(map, w);
        return w.toString();
    }

    public D merge(D other) {
        AFn ignoreNulls = new AFn() {
            public Object invoke(Object arg1, Object arg2) {
                return (arg2 == null) ? arg1 : arg2;
            }
        };
        Object mergedMap = ClojureStuff.MergeWith.invoke(ignoreNulls, map, other.getMap());
        return DynamicObject.wrap(mergedMap, type);
    }

    public D intersect(D arg) {
        return diff(arg, 2);
    }

    public D subtract(D arg) {
        return diff(arg, 0);
    }

    private D diff(D arg, int idx) {
        Object array = ClojureStuff.Diff.invoke(map, arg.getMap());
        Object union = ClojureStuff.Nth.invoke(array, idx);
        if (union == null) union = ClojureStuff.EmptyMap;
        union = Metadata.withTypeMetadata(union, type);
        return DynamicObject.wrap(union, type);
    }

    public D convertAndAssoc(Object key, Object value) {
        return assoc(key, Conversions.javaToClojure(value));
    }

    public D assoc(Object key, Object value) {
        if (value instanceof DynamicObject)
            value = ((DynamicObject) value).getMap();
        return DynamicObject.wrap(ClojureStuff.Assoc.invoke(map, key, value), type);
    }

    public D assocMeta(Object key, Object value) {
        return DynamicObject.wrap(ClojureStuff.VaryMeta.invoke(map, ClojureStuff.Assoc, key, value), type);
    }

    public Object getMetadataFor(Object key) {
        Object meta = ClojureStuff.Meta.invoke(map);
        return ClojureStuff.Get.invoke(meta, key);
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
        Object val = ClojureStuff.Get.invoke(map, key);
        return Conversions.clojureToJava(val, genericReturnType);
    }

    public D validate(D self) {
        Validation.validateInstance(this);
        return self;
    }

    public Object $$noop() {
        return this;
    }

    public Object $$validate() {
        $$customValidate();
        Validation.validateInstance(this);
        return this;
    }
}
