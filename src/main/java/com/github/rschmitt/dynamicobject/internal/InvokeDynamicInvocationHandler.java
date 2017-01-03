package com.github.rschmitt.dynamicobject.internal;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.github.rschmitt.dynamicobject.DynamicObject;

import net.fushizen.invokedynamic.proxy.DynamicInvocationHandler;

public class InvokeDynamicInvocationHandler implements DynamicInvocationHandler {
    private final Class dynamicObjectType;
    
    public InvokeDynamicInvocationHandler(Class dynamicObjectType) {
        this.dynamicObjectType = dynamicObjectType;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CallSite handleInvocation(MethodHandles.Lookup lookup, String methodName, MethodType methodType, MethodHandle superMethod) throws Throwable {
        Class proxyType = methodType.parameterArray()[0];
        MethodHandle mh = null;
        if (superMethod != null && !"validate".equals(methodName) && !"equals".equals(methodName) && !"hashCode".equals(methodName)) {
            mh = superMethod.asType(methodType);
            return new ConstantCallSite(mh);
        }
        if ("equals".equals(methodName)) {
            try {
                mh = lookup.findSpecial(dynamicObjectType, "isEqualTo", methodType(Boolean.class, new Class[] { Object.class }), proxyType).asType(methodType);
            } catch (NoSuchMethodException ex) {
                mh = superMethod.asType(methodType);
                return new ConstantCallSite(mh);
            }
        } else if ("hashCode".equals(methodName)) {
            
            try {
                mh = lookup.findSpecial(dynamicObjectType, "getHashCode", methodType(Integer.class, new Class[] {}), proxyType).asType(methodType);
            } catch (NoSuchMethodException ex) {
                mh = superMethod.asType(methodType);
                return new ConstantCallSite(mh);
            }
            
        } else if ("validate".equals(methodName)) {
            mh = lookup.findSpecial(DynamicObjectInstance.class, "$$validate", methodType(Object.class, new Class[] {}), proxyType).asType(methodType);
        } else if ("$$customValidate".equals(methodName)) {
            try {
                mh = lookup.findSpecial(dynamicObjectType, "validate", methodType(dynamicObjectType), proxyType);
            } catch (NoSuchMethodException ex) {
                mh = lookup.findSpecial(DynamicObjectInstance.class, "$$noop", methodType(Object.class, new Class[] {}), proxyType);
            }
            mh = mh.asType(methodType);
        } else if ("afterDeserialization".equals(methodName)) {
            mh = lookup.findSpecial(DynamicObjectInstance.class, "$$noop", methodType(Object.class, new Class[] {}), proxyType).asType(methodType);
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
