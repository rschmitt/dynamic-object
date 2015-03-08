package com.github.rschmitt.dynamicobject;

import net.fushizen.invokedynamic.proxy.DynamicInvocationHandler;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

import static com.github.rschmitt.dynamicobject.Reflection.*;
import static com.github.rschmitt.dynamicobject.Reflection.isRequired;
import static java.lang.invoke.MethodType.methodType;

public class InvokeDynamicInvocationHandler implements DynamicInvocationHandler {
    private final Class dynamicObjectType;

    public InvokeDynamicInvocationHandler(Class dynamicObjectType) {
        this.dynamicObjectType = dynamicObjectType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CallSite handleInvocation(
            MethodHandles.Lookup lookup,
            String methodName,
            MethodType methodType,
            MethodHandle superMethod
    ) throws Throwable {
        Class proxyType = methodType.parameterArray()[0];
        MethodHandle mh;
        if (superMethod != null) {
            return new ConstantCallSite(superMethod.asType(methodType));
        }
        if ("getMap".equals(methodName)) {
            mh = lookup.findSpecial(DynamicObjectInstance.class, "getMap", methodType(Map.class), proxyType).asType(methodType);
        } else if ("getType".equals(methodName)) {
            mh = lookup.findSpecial(DynamicObjectInstance.class, "getType", methodType(Class.class), proxyType).asType(methodType);
        } else if ("toString".equals(methodName)) {
            mh = lookup.findSpecial(DynamicObjectInstance.class, "toString", methodType(String.class), proxyType).asType(methodType);
        } else if ("hashCode".equals(methodName)) {
            mh = lookup.findSpecial(DynamicObjectInstance.class, "toString", methodType(int.class), proxyType).asType(methodType);
        } else if ("prettyPrint".equals(methodName)) {
            mh = lookup.findSpecial(DynamicObjectInstance.class, "pprint", methodType(Void.TYPE), proxyType).asType(methodType);
        } else if ("toFormattedString".equals(methodName)) {
            mh = lookup.findSpecial(DynamicObjectInstance.class, "toFormattedString", methodType(String.class), proxyType).asType(methodType);
        } else if ("merge".equals(methodName)) {
            mh = lookup.findSpecial(DynamicObjectInstance.class, "merge", methodType(Object.class, Object.class), proxyType).asType(methodType);
        } else if ("intersect".equals(methodName)) {
            mh = lookup.findSpecial(DynamicObjectInstance.class, "intersect", methodType(Object.class, Object.class), proxyType).asType(methodType);
        } else if ("subtract".equals(methodName)) {
            mh = lookup.findSpecial(DynamicObjectInstance.class, "subtract", methodType(Object.class, Object.class), proxyType).asType(methodType);
        } else if ("validate".equals(methodName)) {
            mh = lookup.findSpecial(DynamicObjectInstance.class, "$$validate", methodType(Object.class), proxyType).asType(methodType);
        } else if ("equals".equals(methodName)) {
            mh = lookup.findSpecial(DynamicObjectInstance.class, "equals", methodType(boolean.class, Object.class), proxyType).asType(methodType);
        } else {
            Method method = dynamicObjectType.getMethod(methodName, methodType.dropParameterTypes(0, 1).parameterArray());

            if (isBuilderMethod(method)) {
                Object key = getKeyForBuilder(method);
                if (isMetadataBuilder(method)) {
                    mh = lookup.findSpecial(DynamicObjectInstance.class, "assocMeta", methodType(DynamicObject.class, Object.class, Object.class), proxyType);
                    mh = MethodHandles.insertArguments(mh, 1, key);
                    mh = mh.asType(methodType);
                } else {
                    mh = lookup.findSpecial(DynamicObjectInstance.class, "convertAndAssoc", methodType(DynamicObject.class, Object.class, Object.class), proxyType);
                    mh = MethodHandles.insertArguments(mh, 1, key);
                    mh = mh.asType(methodType);
                }
            } else {
                Object key = getKeyForGetter(method);
                if (isMetadataGetter(method)) {
                    mh = lookup.findSpecial(DynamicObjectInstance.class, "getMetadataFor", methodType(Object.class, Object.class), proxyType);
                    mh = MethodHandles.insertArguments(mh, 1, key);
                    mh = mh.asType(methodType);
                } else {
                    boolean isRequired = isRequired(method);
                    Type genericReturnType = method.getGenericReturnType();
                    mh = lookup.findSpecial(DynamicObjectInstance.class, "invokeGetter", methodType(Object.class, Object.class, boolean.class, Type.class), proxyType);
                    mh = MethodHandles.insertArguments(mh, 1, key, isRequired, genericReturnType);
                    mh = mh.asType(methodType);
                }
            }
        }
        return new ConstantCallSite(mh);
    }

    private boolean isBuilderMethod(Method method) {
        return method.getReturnType().equals(dynamicObjectType) && method.getParameterCount() == 1;
    }
}
