package com.github.rschmitt.dynamicobject;

import clojure.lang.IFn;

import static clojure.java.api.Clojure.read;
import static clojure.java.api.Clojure.var;

class ClojureStuff {
    static final Object EmptyMap = read("{}");
    static final Object EmptySet = read("#{}");
    static final Object EmptyVector = read("[]");
    static final Object Type = read(":type");
    static final Object Readers = read(":readers");
    static final Object Default = read(":default");

    static final IFn Assoc = var("clojure.core", "assoc");
    static final IFn AssocBang = var("clojure.core", "assoc!");
    static final IFn Bigint = var("clojure.core", "bigint");
    static final IFn Biginteger = var("clojure.core", "biginteger");
    static final IFn ConjBang = var("clojure.core", "conj!");
    static final IFn Deref = var("clojure.core", "deref");
    static final IFn Dissoc = var("clojure.core", "dissoc");
    static final IFn Eval = var("clojure.core", "eval");
    static final IFn Get = var("clojure.core", "get");
    static final IFn Memoize = var("clojure.core", "memoize");
    static final IFn MergeWith = var("clojure.core", "merge-with");
    static final IFn Meta = var("clojure.core", "meta");
    static final IFn Name = var("clojure.core", "name");
    static final IFn Nth = var("clojure.core", "nth");
    static final IFn Persistent = var("clojure.core", "persistent!");
    static final IFn PrOn = var("clojure.core", "pr-on");
    static final IFn Read = var("clojure.edn", "read");
    static final IFn ReadString = var("clojure.edn", "read-string");
    static final IFn RemoveMethod = var("clojure.core", "remove-method");
    static final IFn Transient = var("clojure.core", "transient");
    static final IFn WithMeta = var("clojure.core", "with-meta");
    static final IFn VaryMeta = var("clojure.core", "vary-meta");

    static final Object PrintMethod = Deref.invoke(var("clojure.core", "print-method"));
    static final IFn CachedRead = (IFn) Memoize.invoke(var("clojure.edn", "read-string"));
    static final IFn Pprint;
    static final IFn Diff;

    static {
        IFn require = var("clojure.core", "require");
        require.invoke(read("clojure.pprint"));
        require.invoke(read("clojure.data"));

        Pprint = var("clojure.pprint/pprint");
        Diff = var("clojure.data/diff");
    }

    static Object cachedRead(String edn) {
        return CachedRead.invoke(edn);
    }
}
