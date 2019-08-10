package com.github.rschmitt.dynamicobject.internal;

import clojure.lang.IFn;

import java.util.Map;

import static clojure.java.api.Clojure.read;
import static clojure.java.api.Clojure.var;

@SuppressWarnings("rawtypes")
public class ClojureStuff {
    public static final Map EmptyMap = (Map) read("{}");
    public static final Object EmptySet = read("#{}");
    public static final Object EmptyVector = read("[]");
    public static final Object Readers = read(":readers");
    public static final Object Default = read(":default");

    public static final IFn Assoc = var("clojure.core/assoc");
    public static final IFn AssocBang = var("clojure.core/assoc!");
    public static final IFn Bigint = var("clojure.core/bigint");
    public static final IFn Biginteger = var("clojure.core/biginteger");
    public static final IFn ConjBang = var("clojure.core/conj!");
    public static final IFn Deref = var("clojure.core/deref");
    public static final IFn Dissoc = var("clojure.core/dissoc");
    public static final IFn Eval = var("clojure.core/eval");
    public static final IFn Get = var("clojure.core/get");
    public static final IFn Memoize = var("clojure.core/memoize");
    public static final IFn MergeWith = var("clojure.core/merge-with");
    public static final IFn Meta = var("clojure.core/meta");
    public static final IFn Nth = var("clojure.core/nth");
    public static final IFn Persistent = var("clojure.core/persistent!");
    public static final IFn PreferMethod = var("clojure.core/prefer-method");
    public static final IFn PrOn = var("clojure.core/pr-on");
    public static final IFn Read = var("clojure.edn/read");
    public static final IFn ReadString = var("clojure.edn/read-string");
    public static final IFn RemoveMethod = var("clojure.core/remove-method");
    public static final IFn Transient = var("clojure.core/transient");
    public static final IFn VaryMeta = var("clojure.core/vary-meta");

    public static final Object PrintMethod = Deref.invoke(var("clojure.core/print-method"));
    public static final IFn CachedRead = (IFn) Memoize.invoke(var("clojure.edn/read-string"));
    public static final IFn Pprint;
    public static final IFn SimpleDispatch;
    public static final IFn Diff;

    public static final Map clojureReadHandlers;
    public static final Map clojureWriteHandlers;

    static {
        IFn require = var("clojure.core/require");
        require.invoke(read("clojure.pprint"));
        require.invoke(read("clojure.data"));
        require.invoke(read("clojure.data.fressian"));

        Pprint = var("clojure.pprint/pprint");
        Diff = var("clojure.data/diff");

        SimpleDispatch = (IFn) Deref.invoke(var("clojure.pprint/simple-dispatch"));

        clojureReadHandlers = (Map) Deref.invoke(var("clojure.data.fressian/clojure-read-handlers"));
        clojureWriteHandlers = (Map) Deref.invoke(var("clojure.data.fressian/clojure-write-handlers"));
    }

    public static Object cachedRead(String edn) {
        return CachedRead.invoke(edn);
    }
}
