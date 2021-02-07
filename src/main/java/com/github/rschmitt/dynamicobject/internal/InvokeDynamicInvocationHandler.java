package com.github.rschmitt.dynamicobject.internal;

import com.github.rschmitt.dynamicobject.DynamicObject;
import com.github.rschmitt.dynamicobject.internal.indyproxy.DynamicInvocationHandler;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static java.lang.invoke.MethodType.methodType;

@SuppressWarnings("rawtypes")
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
        if (superMethod != null && !"validate".equals(methodName)) {
            mh = superMethod.asType(methodType);
            return new ConstantCallSite(mh);
        }
        if ("validate".equals(methodName)) {
            mh = Validation.buildValidatorFor(dynamicObjectType).asType(methodType);
        } else if ("$$customValidate".equals(methodName)) {
            try {
                mh = lookup.findSpecial(dynamicObjectType, "validate", methodType(dynamicObjectType), proxyType);
            } catch (NoSuchMethodException ex) {
                mh = lookup.findSpecial(DynamicObjectInstance.class, "$$noop", methodType(Object.class, new Class[]{}), proxyType);
            }
            mh = mh.asType(methodType);
        } else if ("afterDeserialization".equals(methodName)) {
            mh = lookup.findSpecial(DynamicObjectInstance.class, "$$noop", methodType(Object.class, new Class[]{}), proxyType).asType(methodType);
        } else {
            Method method = dynamicObjectType.getMethod(methodName, methodType.dropParameterTypes(0, 1).parameterArray());

            if (isBuilderMethod(method)) {
                Object key = Reflection.getKeyForBuilder(method);
                if (Reflection.isMetadataBuilder(method)) {
                    mh = lookup.findSpecial(DynamicObjectInstance.class, "assocMeta", methodType(DynamicObject.class, Object.class, Object.class), proxyType);
                    mh = MethodHandles.insertArguments(mh, 1, key);
                    mh = mh.asType(methodType);
                } else {
                    mh = lookup.findSpecial(DynamicObjectInstance.class, "convertAndAssoc", methodType(DynamicObject.class, Object.class, Object.class), proxyType);
                    mh = MethodHandles.insertArguments(mh, 1, key);
                    mh = mh.asType(methodType);
                }
            } else {
                Object key = Reflection.getKeyForGetter(method);
                if (Reflection.isMetadataGetter(method)) {
                    mh = lookup.findSpecial(DynamicObjectInstance.class, "getMetadataFor", methodType(Object.class, Object.class), proxyType);
                    mh = MethodHandles.insertArguments(mh, 1, key);
                    mh = mh.asType(methodType);
                } else {
                    boolean isRequired = Reflection.isRequired(method);
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
