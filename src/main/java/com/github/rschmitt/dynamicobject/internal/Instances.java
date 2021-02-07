package com.github.rschmitt.dynamicobject.internal;

import com.github.rschmitt.dynamicobject.DynamicObject;
import com.github.rschmitt.dynamicobject.internal.indyproxy.DynamicProxy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.EmptyMap;

@SuppressWarnings("rawtypes")
public class Instances {
    private static final ConcurrentMap<Class, DynamicProxy> proxyCache = new ConcurrentHashMap<>();

    public static <D extends DynamicObject<D>> D newInstance(Class<D> type) {
        return wrap(EmptyMap, type);
    }

    public static <D extends DynamicObject<D>> D wrap(Map map, Class<D> type) {
        if (map == null)
            throw new NullPointerException("A null reference cannot be used as a DynamicObject");
        if (map instanceof DynamicObject)
            return type.cast(map);

        return createIndyProxy(map, type);
    }

    private static <D extends DynamicObject<D>> D createIndyProxy(Map map, Class<D> type) {
        ensureInitialized(type);
        try {
            DynamicProxy dynamicProxy;
            // Use ConcurrentHashMap#computeIfAbsent only when key is not present to avoid locking: JDK-8161372
            if (proxyCache.containsKey(type)) {
                dynamicProxy = proxyCache.get(type);
            } else {
                dynamicProxy = proxyCache.computeIfAbsent(type, Instances::createProxy);
            }
            Object proxy = dynamicProxy
                    .constructor()
                    .invoke(map, type);
            return type.cast(proxy);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // This is to avoid hitting JDK-8062841 in the case where 'type' has a static field of type D
    // that has not yet been initialized.
    private static <D extends DynamicObject<D>> void ensureInitialized(Class<D> c) {
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
