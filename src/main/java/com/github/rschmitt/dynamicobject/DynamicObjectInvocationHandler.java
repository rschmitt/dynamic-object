package com.github.rschmitt.dynamicobject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

import static com.github.rschmitt.dynamicobject.Reflection.*;

class DynamicObjectInvocationHandler<T extends DynamicObject<T>> implements InvocationHandler {
    private static final ConcurrentMap<Method, MethodHandle> methodHandleCache = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Method, Invokee> invocationCache = new ConcurrentHashMap<>();

    private final DynamicObjectInstance<T> instance;

    DynamicObjectInvocationHandler(DynamicObjectInstance<T> instance) {
        this.instance = instance;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return invocationCache.computeIfAbsent(method, this::getInvocation).invoke(instance, proxy, args);
    }

    @SuppressWarnings("unchecked")
    private Invokee getInvocation(Method method) {
        String methodName = method.getName();

        if (method.isDefault()) {
            if (methodName.equals("validate")) {
                return (instance, p, a) -> {
                    instance.validate((T) p);
                    return invokeDefaultMethod(p, method, a);
                };
            }
            return (instance, p, a) -> invokeDefaultMethod(p, method, a);
        }

        if (isBuilderMethod(method)) {
            Object key = getKeyForBuilder(method);
            if (isMetadataBuilder(method))
                return (instance, p, a) -> instance.assocMeta(key, a[0]);
            return (instance, p, a) -> instance.convertAndAssoc(key, a[0]);
        }

        switch (methodName) {
            case "getMap": return (instance, p, a) -> instance.getMap();
            case "getType": return (instance, p, a) -> instance.getType();
            case "toString": return (instance, p, a) -> instance.toString();
            case "hashCode": return (instance, p, a) -> instance.hashCode();
            case "prettyPrint": return (instance, p, a) -> { instance.prettyPrint(); return null; };
            case "toFormattedString": return (instance, p, a) -> instance.toFormattedString();
            case "merge": return (instance, p, a) -> instance.merge((T) a[0]);
            case "intersect": return (instance, p, a) -> instance.intersect((T) a[0]);
            case "subtract": return (instance, p, a) -> instance.subtract((T) a[0]);
            case "validate": return (instance, p, a) -> instance.validate((T) p);
            case "equals": return (instance, p, a) -> instance.equals(a[0]);
            default:
                if (isMetadataGetter(method))
                    return (instance, p, a) -> instance.getMetadataFor(getKeyForGetter(method));
                Object key = Reflection.getKeyForGetter(method);
                boolean isRequired = isRequired(method);
                return (instance, p, a) -> instance.invokeGetter(key, isRequired, method.getGenericReturnType());
        }
    }

    private boolean isBuilderMethod(Method method) {
        return method.getReturnType().equals(instance.getType()) && method.getParameterCount() == 1;
    }

    private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
        return getMethodHandle(method).bindTo(proxy).invokeWithArguments(args);
    }

    private MethodHandle getMethodHandle(Method method) {
        return methodHandleCache.computeIfAbsent(method, DynamicObjectInvocationHandler::createMethodHandle);
    }

    private static MethodHandle createMethodHandle(Method method) {
        try {
            Class<?> declaringClass = method.getDeclaringClass();
            Constructor<MethodHandles.Lookup> lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            lookupConstructor.setAccessible(true);
            int TRUSTED = -1;
            return lookupConstructor.newInstance(declaringClass, TRUSTED).unreflectSpecial(method, declaringClass);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @FunctionalInterface
    public interface Invokee {
        Object invoke(DynamicObjectInstance instance, Object proxy, Object[] args) throws Throwable;
    }
}
