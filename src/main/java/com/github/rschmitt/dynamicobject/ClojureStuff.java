package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

class ClojureStuff {
    static final Object EmptyMap = Clojure.read("{}");
    static final Object EmptySet = Clojure.read("#{}");
    static final Object EmptyVector = Clojure.read("[]");
    static final Object Type = Clojure.read(":type");
    static final Object Readers = Clojure.read(":readers");

    static final IFn Assoc = Clojure.var("clojure.core", "assoc");
    static final IFn AssocBang = Clojure.var("clojure.core", "assoc!");
    static final IFn Bigint = Clojure.var("clojure.core", "bigint");
    static final IFn Biginteger = Clojure.var("clojure.core", "biginteger");
    static final IFn ConjBang = Clojure.var("clojure.core", "conj!");
    static final IFn Deref = Clojure.var("clojure.core", "deref");
    static final IFn Dissoc = Clojure.var("clojure.core", "dissoc");
    static final IFn Eval = Clojure.var("clojure.core", "eval");
    static final IFn Get = Clojure.var("clojure.core", "get");
    static final IFn Memoize = Clojure.var("clojure.core", "memoize");
    static final IFn MergeWith = Clojure.var("clojure.core", "merge-with");
    static final IFn Meta = Clojure.var("clojure.core", "meta");
    static final IFn Name = Clojure.var("clojure.core", "name");
    static final IFn Nth = Clojure.var("clojure.core", "nth");
    static final IFn Persistent = Clojure.var("clojure.core", "persistent!");
    static final IFn PrintString = Clojure.var("clojure.core", "pr-str");
    static final IFn Read = Clojure.var("clojure.edn", "read");
    static final IFn ReadString = Clojure.var("clojure.edn", "read-string");
    static final IFn RemoveMethod = Clojure.var("clojure.core", "remove-method");
    static final IFn Transient = Clojure.var("clojure.core", "transient");
    static final IFn WithMeta = Clojure.var("clojure.core", "with-meta");
    static final IFn VaryMeta = Clojure.var("clojure.core", "vary-meta");

    static final Object PrintMethod = Deref.invoke(Clojure.var("clojure.core", "print-method"));
    static final IFn CachedRead = (IFn) Memoize.invoke(Clojure.var("clojure.edn", "read-string"));
    static final IFn Pprint;
    static final IFn Diff;

    static {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("clojure.pprint"));
        require.invoke(Clojure.read("clojure.data"));

        Pprint = Clojure.var("clojure.pprint/pprint");
        Diff = Clojure.var("clojure.data/diff");
    }

    static Object cachedRead(String edn) {
        return CachedRead.invoke(edn);
    }
}
