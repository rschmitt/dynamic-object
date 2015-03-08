package net.fushizen.invokedynamic.proxy;

import java.lang.invoke.*;

class DefaultInvocationHandler implements DynamicInvocationHandler {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final MethodHandle THROW_UNSUPPORTED;

    static {
        try {
            THROW_UNSUPPORTED = LOOKUP.findStatic(DefaultInvocationHandler.class,
                    "throwUnsupported",
                    MethodType.methodType(Object.class));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private static Object throwUnsupported() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CallSite handleInvocation(
            MethodHandles.Lookup proxyLookup,
            String methodName,
            MethodType methodType,
            MethodHandle superMethod
    ) {
        if (superMethod != null) {
            return new ConstantCallSite(superMethod.asType(methodType));
        }

        return new ConstantCallSite(
                MethodHandles.dropArguments(THROW_UNSUPPORTED, 0, methodType.parameterList()).asType(methodType)
        );
    }
}
