package com.github.rschmitt.dynamicobject.internal;

import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.EmptyMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.github.rschmitt.dynamicobject.DynamicObject;

import net.fushizen.invokedynamic.proxy.DynamicProxy;

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

        return createIndyProxy(map, type);
    }

    private static <D extends DynamicObject<D>> D createIndyProxy(Map map, Class<D> type) {
        try {
            Object proxy = proxyCache.computeIfAbsent(type, Instances::createProxy)
                    .constructor()
                    .invoke(map, type);
            return type.cast(proxy);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static DynamicProxy createProxy(Class dynamicObjectType) {
        String[] slices = dynamicObjectType.getName().split("\\.");
        String name = slices[slices.length - 1] + "Impl";
        try {
            return DynamicProxy.builder()
                    .withInterfaces(dynamicObjectType, CustomValidationHook.class)
                    .withSuperclass(DynamicObjectInstance.class)
                    .withInvocationHandler(new InvokeDynamicInvocationHandler(dynamicObjectType))
                    .withConstructor(Map.class, Class.class)
                    .withPackageName(dynamicObjectType.getPackage().getName())
                    .withClassName(name)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
