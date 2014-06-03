package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.*;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicObjects {
    private static volatile IPersistentMap readers = PersistentHashMap.EMPTY;
    private static final ConcurrentHashMap<Class<?>, String> recordTagCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, EdnTranslator<?>> translatorCache = new ConcurrentHashMap<>();

    private static IPersistentMap getReadersAsOptions() {
        return PersistentHashMap.EMPTY.assoc(Clojure.read(":readers"), readers);
    }

    static <T extends DynamicObject<T>> String serialize(T o) {
        IFn prstr = Clojure.var("clojure.core", "pr-str");
        Class<T> type = o.getType();
        if (translatorCache.containsKey(type))
            return (String) prstr.invoke(o);
        IPersistentMap map = o.getMap();
        return (String) prstr.invoke(map);
    }

    @SuppressWarnings("unchecked")
    static <T extends DynamicObject<T>> T deserialize(String edn, Class<T> clazz) {
        Object obj = EdnReader.readString(edn, getReadersAsOptions());
        if (obj instanceof DynamicObject)
            return (T) obj;
        IPersistentMap map = (IPersistentMap) obj;
        return wrap(map, clazz);
    }

    @SuppressWarnings("unchecked")
    static <T extends DynamicObject<T>> T wrap(IPersistentMap map, Class<T> clazz) {
        try {
            Constructor<MethodHandles.Lookup> lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            lookupConstructor.setAccessible(true);

            return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class<?>[]{clazz},
                    new DynamicObjectInvocationHandler<>(map, clazz, lookupConstructor));
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    static <T extends DynamicObject<T>> T newInstance(Class<T> clazz) {
        IPersistentMap meta = PersistentHashMap.EMPTY.assoc(Clojure.read(":type"),
                Clojure.read(":" + clazz.getCanonicalName()));
        return wrap(PersistentHashMap.EMPTY.withMeta(meta), clazz);
    }

    static <T> void registerType(Class<T> clazz, EdnTranslator<T> translator) {
        synchronized (DynamicObject.class) {
            translatorCache.put(clazz, translator);

            // install as a reader
            readers = readers.assocEx(Clojure.read(translator.getTag()), translator);

            // install multimethod for writing
            Var varPrintMethod = (Var) Clojure.var("clojure.core", "print-method");
            MultiFn printMethod = (MultiFn) varPrintMethod.get();
            printMethod.addMethod(clazz, translator);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> void deregisterType(Class<T> clazz) {
        synchronized (DynamicObject.class) {
            EdnTranslator<T> translator = (EdnTranslator<T>) translatorCache.get(clazz);

            // uninstall reader
            readers = readers.without(Clojure.read(translator.getTag()));

            // uninstall print-method multimethod
            Var varPrintMethod = (Var) Clojure.var("clojure.core", "print-method");
            MultiFn printMethod = (MultiFn) varPrintMethod.get();
            printMethod.removeMethod(clazz);

            translatorCache.remove(clazz);
        }
    }

    static <T extends DynamicObject<T>> void registerTag(Class<T> clazz, String tag) {
        synchronized (DynamicObject.class) {
            recordTagCache.put(clazz, tag);
            readers = readers
                    .assocEx(Clojure.read(tag),
                            new AFn() {
                                @Override
                                public Object invoke(Object obj) {
                                    IObj mapWithMeta = (IObj) obj;
                                    IPersistentMap meta = mapWithMeta.meta();
                                    if (meta == null)
                                        meta = PersistentHashMap.EMPTY;
                                    IPersistentMap newMeta = meta.assoc(Clojure.read(":type"),
                                            Clojure.read(":" + clazz.getCanonicalName()));
                                    return mapWithMeta.withMeta(newMeta);
                                }
                            }
                    );


            Var varPrintMethod = (Var) Clojure.var("clojure.core", "print-method");
            MultiFn printMethod = (MultiFn) varPrintMethod.get();
            printMethod.addMethod(Clojure.read(":" + clazz.getCanonicalName()), new AFn() {
                @Override
                public Object invoke(Object arg1, Object arg2) {
                    Writer writer = (Writer) arg2;
                    IObj obj = (IObj) arg1;
                    obj = obj.withMeta(null);
                    try {
                        writer.write(String.format("#%s%s", tag, obj.toString()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                }
            });
        }
    }

    static <T extends DynamicObject<T>> void deregisterTag(Class<T> clazz) {
        String tag = recordTagCache.get(clazz);
        readers = readers.without(Clojure.read(tag));
        recordTagCache.remove(clazz);

        Var varPrintMethod = (Var) Clojure.var("clojure.core", "print-method");
        MultiFn printMethod = (MultiFn) varPrintMethod.get();
        printMethod.removeMethod(Clojure.read(":" + clazz.getCanonicalName()));
    }
}
