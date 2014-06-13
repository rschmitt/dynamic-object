package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

class ClojureStuff {
    static final Object EMPTY_MAP = Clojure.read("{}");
    static final Object EMPTY_SET = Clojure.read("#{}");
    static final Object EMPTY_VECTOR = Clojure.read("[]");
    static final Object TYPE = Clojure.read(":type");
    static final Object READERS = Clojure.read(":readers");

    static final IFn ASSOC = Clojure.var("clojure.core", "assoc");
    static final IFn ASSOC_BANG = Clojure.var("clojure.core", "assoc!");
    static final IFn CONJ_BANG = Clojure.var("clojure.core", "conj!");
    static final IFn COUNT = Clojure.var("clojure.core", "count");
    static final IFn DEREF = Clojure.var("clojure.core", "deref");
    static final IFn DISSOC = Clojure.var("clojure.core", "dissoc");
    static final IFn EVAL = Clojure.var("clojure.core", "eval");
    static final IFn FIRST = Clojure.var("clojure.core", "first");
    static final IFn GET = Clojure.var("clojure.core", "get");
    static final IFn KEY = Clojure.var("clojure.core", "key");
    static final IFn MEMOIZE = Clojure.var("clojure.core", "memoize");
    static final IFn MERGE_WITH = Clojure.var("clojure.core", "merge-with");
    static final IFn META = Clojure.var("clojure.core", "meta");
    static final IFn NAME = Clojure.var("clojure.core", "name");
    static final IFn NTH = Clojure.var("clojure.core", "nth");
    static final IFn PERSISTENT = Clojure.var("clojure.core", "persistent!");
    static final IFn PRINT_STRING = Clojure.var("clojure.core", "pr-str");
    static final IFn READ_STRING = Clojure.var("clojure.edn", "read-string");
    static final IFn REMOVE_METHOD = Clojure.var("clojure.core", "remove-method");
    static final IFn REST = Clojure.var("clojure.core", "rest");
    static final IFn TRANSIENT = Clojure.var("clojure.core", "transient");
    static final IFn VAL = Clojure.var("clojure.core", "val");
    static final IFn WITH_META = Clojure.var("clojure.core", "with-meta");

    static final Object PRINT_METHOD = DEREF.invoke(Clojure.var("clojure.core", "print-method"));
    static final IFn CACHED_READ = (IFn) MEMOIZE.invoke(Clojure.var("clojure.edn", "read-string"));
    static final IFn PPRINT;
    static final IFn DIFF;

    static {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("clojure.pprint"));
        require.invoke(Clojure.read("clojure.data"));

        PPRINT = Clojure.var("clojure.pprint/pprint");
        DIFF = Clojure.var("clojure.data/diff");
    }

    static Object cachedRead(String edn) {
        return CACHED_READ.invoke(edn);
    }
}
