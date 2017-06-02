package com.github.rschmitt.dynamicobject.internal;

import clojure.lang.Cons;
import clojure.lang.IMeta;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import com.github.rschmitt.collider.ClojureMap;
import net.fushizen.invokedynamic.proxy.DynamicProxy;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Map;

import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;

/**
 * A map class that wraps some other Map; however, it also implements IPersistentMap and IObj, creating a copy of the
 * original map when IPersistentMap or IObj methods such as assoc are invoked.
 */
abstract class WrappingMap implements IMeta {
    private static final IPersistentMap EMPTY_MAP = PersistentHashMap.create();

    protected final Map backingMap;

    protected WrappingMap(Map backingMap) {
        this.backingMap = backingMap;
    }

    @Override
    public IPersistentMap meta() {
        // Avoid copying the map if we're doing a read-only metadata access.
        return EMPTY_MAP;
    }

    static Map create(Map other) {
        try {
            return (Map)proxy_ctor.invokeExact(other);
        } catch (Throwable t) {
            throw new Error("unexpected exception", t);
        }
    }

    private static final MethodHandle get_backingMap;
    private static final MethodHandle proxy_ctor;

    static {
        try {
            get_backingMap = MethodHandles.lookup().findGetter(WrappingMap.class, "backingMap", Map.class);
            proxy_ctor = DynamicProxy.builder()
                        .withConstructor(Map.class)
                        .withSuperclass(WrappingMap.class)
                        .withInterfaces(IPersistentMap.class, IObj.class, Map.class)
                        .withInvocationHandler(WrappingMap::invocationHandler)
                        .build()
                        .constructor()
                        .asType(methodType(Map.class, Map.class));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private static CallSite invocationHandler(
            MethodHandles.Lookup lookup,
            String methodName,
            MethodType methodType,
            MethodHandle superHandle
    ) throws Throwable {
        // Forward calls that are overridden on WrappingMap to that implementation
        try {
            Method m = WrappingMap.class.getDeclaredMethod(methodName, methodType.dropParameterTypes(0, 1).parameterArray());

            // since the method exists, we can just use superHandle
            return new ConstantCallSite(superHandle.asType(methodType));
        } catch (NoSuchMethodException e) {
            // continue
        }

        CallSite result;

        // Forward calls that are declared on Map, or Object to the backing map.
        result = forwardCalls(Map.class, methodName, methodType);
        if (result != null) return result;

        result = forwardCalls(Object.class, methodName, methodType);
        if (result != null) return result;

        // Any other calls are IPersistentMap calls. We'll want to construct a PersistentHashMap and reinvoke the call
        // on it.
        MethodHandle makeMap = MethodHandles.lookup().findVirtual(WrappingMap.class, "createPersistentMap", methodType(IPersistentMap.class));

        MethodType targetMethodType = methodType.dropParameterTypes(0, 1);

        MethodHandle target;
        try {
            target = publicLookup().findVirtual(IPersistentMap.class, methodName, targetMethodType);
        } catch (NoSuchMethodException e) {
            // It's not on IPersistentMap, so try IObj instead
            target = publicLookup().findVirtual(IObj.class, methodName, targetMethodType);
            // If we're successful, we need to do an asType cast since IPersistentMap doesn't extend IObj
            makeMap = makeMap.asType(methodType(IObj.class, WrappingMap.class));
        }

        MethodHandle createMapAndForward = MethodHandles.filterArguments(target, 0, makeMap);

        return new ConstantCallSite(createMapAndForward.asType(methodType));
    }

    private static CallSite forwardCalls(
            Class<?> klass,
            String methodName,
            MethodType methodType
    ) throws Throwable {
        try {
            MethodHandle mapHandle = publicLookup().findVirtual(klass, methodName, methodType.changeParameterType(0, Map.class));
            // ok, this is a call to the class in question, we just need to look up the backing map and invoke the
            // method on it instead
            MethodHandle combinedHandle = MethodHandles.filterArguments(mapHandle, 0, get_backingMap);

            return new ConstantCallSite(combinedHandle.asType(methodType));
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @SuppressWarnings("unused") // invoked by reflection
    protected IPersistentMap createPersistentMap() {
        return PersistentHashMap.create(backingMap);
    }

    @SuppressWarnings("unused") // invoked by reflection
    private static Object throwUnsupported() {
        throw new UnsupportedOperationException();
    }
}
