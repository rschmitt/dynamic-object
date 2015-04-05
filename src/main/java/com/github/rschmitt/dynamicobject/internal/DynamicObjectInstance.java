package com.github.rschmitt.dynamicobject.internal;

import static java.lang.String.format;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.github.rschmitt.dynamicobject.DynamicObject;

import clojure.lang.AFn;
import clojure.lang.Associative;
import clojure.lang.IHashEq;
import clojure.lang.IMapEntry;
import clojure.lang.IObj;
import clojure.lang.IPersistentCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.ISeq;
import clojure.lang.MapEquivalence;
import clojure.lang.Seqable;

public abstract class DynamicObjectInstance<D extends DynamicObject<D>> extends AFn implements Map, IPersistentMap, IObj, MapEquivalence, IHashEq, DynamicObjectPrintHook, CustomValidationHook<D> {
    private static final Object Default = new Object();
    private static final Object Null = new Object();

    Map map;
    Class<D> type;
    private final ConcurrentHashMap valueCache = new ConcurrentHashMap();

    public DynamicObjectInstance() {
    }

    DynamicObjectInstance(Map map, Class<D> type) {
        this.map = map;
        this.type = type;
    }

    public Map getMap() {
        return map;
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

    @Override
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
        Map mergedMap = (Map) ClojureStuff.MergeWith.invoke(ignoreNulls, map, other.getMap());
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
        return DynamicObject.wrap((Map) union, type);
    }

    public D convertAndAssoc(Object key, Object value) {
        return (D) assoc(key, Conversions.javaToClojure(value));
    }

    @Override
    public IPersistentMap assoc(Object key, Object value) {
        return (DynamicObjectInstance) DynamicObject.wrap((Map) ClojureStuff.Assoc.invoke(map, key, value), type);
    }

    public D assocMeta(Object key, Object value) {
        return DynamicObject.wrap((Map) ClojureStuff.VaryMeta.invoke(map, ClojureStuff.Assoc, key, value), type);
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

    public Object $$noop() {
        return this;
    }

    public Object $$validate() {
        Validation.validateInstance(this);
        return $$customValidate();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return map.get(key);
    }

    @Override
    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set keySet() {
        return map.keySet();
    }

    @Override
    public Collection values() {
        return map.values();
    }

    @Override
    public Set<Entry> entrySet() {
        return map.entrySet();
    }

    @Override
    public IMapEntry entryAt(Object key) {
        return ((Associative) map).entryAt(key);
    }

    @Override
    public Object valAt(Object key) {
        return ((Associative) map).valAt(key);
    }

    @Override
    public Object valAt(Object key, Object notFound) {
        return ((Associative) map).valAt(key, notFound);
    }

    @Override
    public int count() {
        return ((IPersistentCollection) map).count();
    }

    @Override
    public IPersistentCollection cons(Object o) {
        return ((IPersistentCollection) map).cons(o);
    }

    @Override
    public IPersistentCollection empty() {
        return (DynamicObjectInstance) DynamicObject.wrap(ClojureStuff.EmptyMap, type);
    }

    @Override
    public boolean equiv(Object o) {
        return ((IPersistentCollection) map).equiv(o);
    }

    @Override
    public ISeq seq() {
        return ((Seqable) map).seq();
    }

    @Override
    public IPersistentMap assocEx(Object key, Object val) {
        Object newMap = ((IPersistentMap) map).assocEx(key, val);
        return (DynamicObjectInstance) DynamicObject.wrap((Map) newMap, type);
    }

    @Override
    public IPersistentMap without(Object key) {
        Object newMap = ((IPersistentMap) map).without(key);
        return (DynamicObjectInstance) DynamicObject.wrap((Map) newMap, type);
    }

    @Override
    public IPersistentMap meta() {
        return (IPersistentMap) ClojureStuff.Meta.invoke(map);
    }

    @Override
    public IObj withMeta(IPersistentMap meta) {
        Object newMap = ClojureStuff.VaryMeta.invoke(map, meta);
        return (DynamicObjectInstance) DynamicObject.wrap((Map) newMap, type);
    }

    @Override
    public int hasheq() {
        return ((IHashEq) map).hasheq();
    }

    @Override
    public Object invoke(Object arg1) {
        return valAt(arg1);
    }

    @Override
    public Object invoke(Object arg1, Object notFound) {
        return valAt(arg1, notFound);
    }
}
