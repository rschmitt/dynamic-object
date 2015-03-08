package com.github.rschmitt.dynamicobject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.github.rschmitt.dynamicobject.Reflection.*;

class DynamicObjectInvocationHandler<T extends DynamicObject<T>> implements InvocationHandler {
    private static final ConcurrentMap<Method, MethodHandle> methodHandleCache = new ConcurrentHashMap<>();

    private final DynamicObjectInstance instance;

    DynamicObjectInvocationHandler(Object map, Class<T> type) {
        this.instance = new DynamicObjectInstance<>(map, type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if (method.isDefault()) {
            if (methodName.equals("validate"))
                instance.validate((T) proxy);
            return invokeDefaultMethod(proxy, method, args);
        }

        if (isBuilderMethod(method))
            return invokeBuilderMethod(method, args);

        switch (methodName) {
            case "getMap": return instance.getMap();
            case "getType": return instance.getType();
            case "toString": return instance.toString();
            case "hashCode": return instance.hashCode();
            case "prettyPrint": instance.prettyPrint(); return null;
            case "toFormattedString": return instance.toFormattedString();
            case "merge": return instance.merge((T) args[0]);
            case "intersect": return instance.intersect((T) args[0]);
            case "subtract": return instance.subtract((T) args[0]);
            case "validate": return instance.validate((T) proxy);
            case "equals": return instance.equals(args[0]);
            default:
                return invokeGetterMethod(method);
        }
    }

    private boolean isBuilderMethod(Method method) {
        return method.getReturnType().equals(instance.getType()) && method.getParameterCount() == 1;
    }

    private Object invokeBuilderMethod(Method method, Object[] args) {
        Object key = getKeyForBuilder(method);
        if (isMetadataBuilder(method))
            return instance.assocMeta(key, args[0]);
        return instance.assoc(key, Conversions.javaToClojure(args[0]));
    }

    private Object invokeGetterMethod(Method method) {
        if (isMetadataGetter(method))
            return instance.getMetadataFor(getKeyForGetter(method));
        Object key = Reflection.getKeyForGetter(method);
        boolean isRequired = isRequired(method);
        return instance.invokeGetter(key, isRequired, method.getGenericReturnType());
    }

    private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
        return getMethodHandle(method)
                .bindTo(proxy)
                .invokeWithArguments(args);
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
}
