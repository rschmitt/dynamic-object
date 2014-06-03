package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

class DynamicObjectInvocationHandler<T extends DynamicObject<T>> implements InvocationHandler {
    private static final IFn GET = Clojure.var("clojure.core", "get");
    private static final IFn CONTAINS_KEY = Clojure.var("clojure.core", "contains?");
    private static final IFn ASSOC = Clojure.var("clojure.core", "assoc");
    private static final IFn DISSOC = Clojure.var("clojure.core", "dissoc");

    private static final IFn PPRINT;

    static {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("clojure.pprint"));

        PPRINT = Clojure.var("clojure.pprint/pprint");
    }

    private final Object map;
    private final Class<T> type;
    private final Constructor<MethodHandles.Lookup> lookupConstructor;

    DynamicObjectInvocationHandler(Object map, Class<T> type, Constructor<MethodHandles.Lookup> lookupConstructor) {
        this.map = map;
        this.type = type;
        this.lookupConstructor = lookupConstructor;
    }

    private T assoc(String key, Object value) {
        Object keyword = Clojure.read(":" + key);
        if (value instanceof DynamicObject)
            value = ((DynamicObject) value).getMap();
        return DynamicObject.wrap(ASSOC.invoke(map, keyword, value), type);
    }

    private T assocEx(String key, Object value) {
        Object keyword = Clojure.read(":" + key);
        if ((boolean) CONTAINS_KEY.invoke(map, keyword)) {
            throw new RuntimeException("");
        }

        return assoc(key, value);
    }

    private T without(String key) {
        Object keyword = Clojure.read(":" + key);
        return DynamicObject.wrap(DISSOC.invoke(map, keyword), type);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if (method.getReturnType().equals(type) && (args != null && args.length > 0))
            return assoc(methodName, args[0]);

        if (method.isDefault())
            return invokeDefaultMethod(proxy, method, args);

        switch (methodName) {
            case "getMap":
                return map;
            case "getType":
                return type;
            case "assoc":
                return assoc((String) args[0], args[1]);
            case "assocEx":
                return assocEx((String) args[0], args[1]);
            case "dissoc":
                return without((String) args[0]);
            case "toString":
                return map.toString();
            case "hashCode":
                return map.hashCode();
            case "prettyPrint":
                PPRINT.invoke(map);
                return null;
            case "toFormattedString":
                Writer w = new StringWriter();
                PPRINT.invoke(map, w);
                return w.toString();
            case "equals":
                Object other = args[0];
                if (other instanceof DynamicObject)
                    return map.equals(((DynamicObject) other).getMap());
                else
                    return method.invoke(map, args);
            default:
                return getValueFor(method);
        }
    }

    private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();
        int TRUSTED = -1;
        return lookupConstructor.newInstance(declaringClass, TRUSTED)
                .unreflectSpecial(method, declaringClass)
                .bindTo(proxy)
                .invokeWithArguments(args);
    }

    @SuppressWarnings("unchecked")
    private Object getValueFor(Method method) {
        String methodName = method.getName();
        Object keywordKey = Clojure.read(":" + methodName);
        Object val = GET.invoke(map, keywordKey);
        if (val == null)
            val = getNonDefaultValue(method);
        if (val == null)
            return null;
        Class<?> returnType = method.getReturnType();
        if (returnType.equals(int.class) || returnType.equals(Integer.class))
            return returnInt(val);
        if (returnType.equals(float.class) || returnType.equals(Float.class))
            return returnFloat(val);
        if (returnType.equals(short.class) || returnType.equals(Short.class))
            return returnShort(val);
        if (DynamicObject.class.isAssignableFrom(returnType)) {
            Class<T> dynamicObjectType = (Class<T>) returnType;
            Object keyword = Clojure.read(":" + methodName);
            return DynamicObject.wrap(GET.invoke(map, keyword), dynamicObjectType);
        }
        return val;
    }

    private float returnFloat(Object val) {
        if (val instanceof Float)
            return (Float) val;
        return ((Double) val).floatValue();
    }

    private int returnInt(Object val) {
        if (val instanceof Integer)
            return (Integer) val;
        else return ((Long) val).intValue();
    }

    private short returnShort(Object val) {
        if (val instanceof Short)
            return (Short) val;
        if (val instanceof Integer)
            return ((Integer) val).shortValue();
        else return ((Long) val).shortValue();
    }

    private Object getNonDefaultValue(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation.annotationType().equals(Key.class)) {
                String key = ((Key) annotation).value();
                if (key.charAt(0) != ':')
                    key = ":" + key;
                return GET.invoke(map, Clojure.read(key));
            }
        }
        return null;
    }
}
