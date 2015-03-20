package com.github.rschmitt.dynamicobject.internal;

import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.EmptyMap;

import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.github.rschmitt.dynamicobject.DynamicObject;
import net.fushizen.invokedynamic.proxy.DynamicProxy;

public class Instances {
    private static final ConcurrentMap<Class, DynamicProxy> proxyCache = new ConcurrentHashMap<>();
    public static boolean USE_INVOKEDYNAMIC = true;

    public static <D extends DynamicObject<D>> D newInstance(Class<D> type) {
        return wrap(Metadata.withTypeMetadata(EmptyMap, type), type);
    }

    @SuppressWarnings("unchecked")
    public static <T> T wrap(Object map, Class<T> type) {
        if (map == null)
            throw new NullPointerException("A null reference cannot be used as a DynamicObject");
        Class<?> typeMetadata = Metadata.getTypeMetadata(map);
        if (typeMetadata != null && !type.equals(typeMetadata))
            throw new ClassCastException(String.format("Attempted to wrap a map tagged as %s in type %s",
                    typeMetadata.getSimpleName(), type.getSimpleName()));

        if (USE_INVOKEDYNAMIC)
            return createIndyProxy(map, type);
        else
            return createReflectionProxy(map, type);
    }

    private static <T> T createIndyProxy(Object map, Class<T> type) {
        T t = (T) proxyCache.computeIfAbsent(type, Instances::createProxy).supplier().get();
        DynamicObjectInstance i = (DynamicObjectInstance) t;
        i.map = map;
        i.type = type;
        return t;
    }

    private static <T, D extends DynamicObject<D>> T createReflectionProxy(Object map, Class<T> type) {
        DynamicObjectInstance<D> instance = new DynamicObjectInstance<D>() {
            @Override
            public D $$customValidate() {
                return null;
            }
        };
        instance.type = (Class<D>) type;
        instance.map = map;
        return (T) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{type},
                new DynamicObjectInvocationHandler<>(instance)
        );
    }

    private static DynamicProxy createProxy(Class dynamicObjectType) {
        try {
            return DynamicProxy.builder()
                    .withInterfaces(dynamicObjectType, CustomValidationHook.class)
                    .withSuperclass(DynamicObjectInstance.class)
                    .withInvocationHandler(new InvokeDynamicInvocationHandler(dynamicObjectType))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
