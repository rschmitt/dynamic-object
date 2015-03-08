package net.fushizen.invokedynamic.proxy;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public interface DynamicInvocationHandler {
    /**
     * This callback is invoked the first time a proxy method is invoked. It should return a CallSite to be bound to the
     * proxy method in question. The CallSite's method arguments (which can also be inspected via methodType) will
     * consist of the arguments of the interface or superclass method in question, plus a prepended argument for the
     * proxy itself.
     * <br>
     * Note that the type of the method bound to the callsite must exactly match methodType. Using
     * {@link java.lang.invoke.MethodHandle#asType} is recommended.
     * <br>
     * If multiple calls race, it's possible this method may be invoked multiple times for the same method. In this case,
     * one of the returns will be selected arbitrarily and used for all calls.
     *
     * @param proxyLookup A Lookup instance with the access rights of the proxy class. This can be used to look up
     *                    proxy superclass methods.
     * @param methodName  The name of the proxy method that is being invoked
     * @param methodType  The type of the callee (same as the type of the proxy method, but with the proxy instance
     *                    itself prepended)
     * @param superMethod If an unambiguous supermethod was found, this has a handle to that supermethod. Otherwise,
     *                    this is null.
     * @return A call site to bind to the proxy method.
     */
    public CallSite handleInvocation(MethodHandles.Lookup proxyLookup, String methodName, MethodType methodType, MethodHandle superMethod)
            throws Throwable;
}
