package com.github.rschmitt.dynamicobject.internal;

import clojure.lang.IPersistentMap;
import com.github.rschmitt.dynamicobject.DynamicObject;
import net.fushizen.invokedynamic.proxy.DynamicProxy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.EmptyMap;

public class Instances {
    private static final ConcurrentMap<Class, DynamicProxy> proxyCache = new ConcurrentHashMap<>();

    public static <D extends DynamicObject<D>> D newInstance(Class<D> type) {
        return wrap(EmptyMap, type);
    }

    @SuppressWarnings("unchecked")
    public static <D extends DynamicObject<D>> D wrap(Map map, Class<D> type) {
        if (map == null)
            throw new NullPointerException("A null reference cannot be used as a DynamicObject");
        if (map instanceof DynamicObject)
            return type.cast(map);

        return createIndyProxy(convertMap(map), type);
    }

    private static Map convertMap(Map map) {
        if (map instanceof IPersistentMap) {
            return map;
        }

        return (Map) WrappingMap.create(map);
    }

    private static <D extends DynamicObject<D>> D createIndyProxy(Map map, Class<D> type) {
        ensureInitialized(type);
        try {
            Object proxy = proxyCache.computeIfAbsent(type, Instances::createProxy)
                    .constructor()
                    .invoke(map, type);
            return type.cast(proxy);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // This is to avoid hitting JDK-8062841 in the case where 'type' has a static field of type D
    // that has not yet been initialized.
    private static synchronized <D extends DynamicObject<D>> void ensureInitialized(Class<D> c) {
        if (!proxyCache.containsKey(c))
            load(c);
    }

    private static void load(Class<?> c) {
        try {
            Class.forName(c.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static DynamicProxy createProxy(Class dynamicObjectType) {
        String[] slices = dynamicObjectType.getName().split("\\.");
        String name = slices[slices.length - 1] + "Impl";
        try {
            DynamicProxy.Builder builder = DynamicProxy.builder()
                    .withInterfaces(dynamicObjectType, CustomValidationHook.class)
                    .withSuperclass(DynamicObjectInstance.class)
                    .withInvocationHandler(new InvokeDynamicInvocationHandler(dynamicObjectType))
                    .withConstructor(Map.class, Class.class)
                    .withPackageName(dynamicObjectType.getPackage().getName())
                    .withClassName(name);
            try {
                Class<?> iMapIterable = Class.forName("clojure.lang.IMapIterable");
                builder = builder.withInterfaces(iMapIterable);
            } catch (ClassNotFoundException ignore) {}
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
