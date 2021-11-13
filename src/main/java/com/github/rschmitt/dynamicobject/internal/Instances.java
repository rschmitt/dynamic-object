package com.github.rschmitt.dynamicobject.internal;

import com.github.rschmitt.dynamicobject.DynamicObject;
import com.github.rschmitt.dynamicobject.internal.indyproxy.DynamicProxy;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.EmptyMap;
import static net.bytebuddy.matcher.ElementMatchers.is;

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

//        return createIndyProxy(map, type);
        return createBBProxy(map, type);
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

    private static <D extends DynamicObject<D>> D createBBProxy(Map map, Class<D> dynamicObjectType) {
        DynamicType.Builder<DynamicObjectInstance> builder = new ByteBuddy()
                .subclass(DynamicObjectInstance.class)
                .implement(dynamicObjectType, CustomValidationHook.class);

        try {
            boolean hasCustomValidate = false;
            boolean hasDeserializationHook = false;
            for (Method method : dynamicObjectType.getDeclaredMethods()) {
                System.out.println("method: " + method.toString());
                String methodName = method.getName();
                if ("validate".equals(methodName)) {
                    hasCustomValidate = true;
                } else if ("$$customValidate".equals(methodName)) {
//                    hasCustomValidate = true;
                } else if ("afterDeserialization".equals(methodName)) {
                    hasDeserializationHook = true;
                } else if (method.isDefault()) {
                    // custom method
                } else if (isBuilderMethod(method, dynamicObjectType)) {
                    Object key = Reflection.getKeyForBuilder(method);
                    if (Reflection.isMetadataBuilder(method)) {
                        System.out.println("-> binding metadata builder");
                        builder = builder.method(is(method))
                                .intercept(MethodCall.invoke(DynamicObjectInstance.class.getMethod("assocMeta",
                                                Object.class, Object.class))
                                        .with(key)
                                        .withArgument(0)
                                        .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
                    } else {
                        System.out.println("-> binding value builder");
                        builder = builder.method(is(method))
                                .intercept(MethodCall.invoke(DynamicObjectInstance.class.getMethod("convertAndAssoc",
                                                Object.class, Object.class))
                                        .with(key)
                                        .withArgument(0)
                                        .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
                    }
                } else {
                    Object key = Reflection.getKeyForGetter(method);
                    if (Reflection.isMetadataGetter(method)) {
                        System.out.println("-> binding metadata getter");
                        builder = builder.method(is(method))
                                .intercept(MethodCall.invoke(DynamicObjectInstance.class.getMethod("getMetadataFor",
                                                Object.class))
                                        .with(key)
                                        .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
                    } else {
                        System.out.println("-> binding value getter");
                        boolean isRequired = Reflection.isRequired(method);
                        Type genericReturnType = method.getGenericReturnType();

                        builder = builder.method(is(method))
                                .intercept(MethodCall.invoke(DynamicObjectInstance.class.getMethod("invokeGetter",
                                                Object.class, boolean.class, Type.class))
                                        .with(key, isRequired, genericReturnType)
                                        .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
                    }
                }
            }

            if (!hasDeserializationHook) {
                builder = builder.method(ElementMatchers.named("afterDeserialization"))
                        .intercept(MethodCall.invoke(DynamicObjectInstance.class.getMethod("$$noop"))
                                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
            }
            if (!hasCustomValidate) {
                builder = builder.method(ElementMatchers.named("validate"))
                        .intercept(MethodCall.invoke(DynamicObjectInstance.class.getMethod("$$noop"))
                                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
            }

            Class<?> dynamicType = builder
                    .make()
                    .load(dynamicObjectType.getClassLoader())
                    .getLoaded();

            Constructor<?> constructor = dynamicType.getConstructor(Map.class, Class.class);
            return (D) constructor.newInstance(map, dynamicObjectType);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static <D> boolean isBuilderMethod(Method method, Class<D> dynamicObjectType) {
        return method.getReturnType().equals(dynamicObjectType) && method.getParameterCount() == 1;
    }
}
