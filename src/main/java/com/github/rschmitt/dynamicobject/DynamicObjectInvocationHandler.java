package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.IMapEntry;
import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

class DynamicObjectInvocationHandler<T> implements InvocationHandler {
    private static final IFn PPRINT;

    static {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("clojure.pprint"));

        PPRINT = Clojure.var("clojure.pprint/pprint");
    }

    private final IPersistentMap map;
    private final Class<T> clazz;

    DynamicObjectInvocationHandler(IPersistentMap map, Class<T> clazz) {
        this.map = map;
        this.clazz = clazz;
    }

    private T assoc(String key, Object value) {
        Keyword keyword = Keyword.intern(key);
        if (value instanceof DynamicObject)
            value = ((DynamicObject) value).getMap();
        IPersistentMap newMap = map.assoc(keyword, value);
        return DynamicObject.wrap(newMap, clazz);
    }

    private T assocEx(String key, Object value) {
        Keyword keyword = Keyword.intern(key);
        if (value instanceof DynamicObject)
            value = ((DynamicObject) value).getMap();
        IPersistentMap newMap = map.assocEx(keyword, value);
        return DynamicObject.wrap(newMap, clazz);
    }

    private T without(String key) {
        Keyword keyword = Keyword.intern(key);
        return DynamicObject.wrap(map.without(keyword), clazz);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if (method.getReturnType().equals(clazz) && args.length > 0)
            return assoc(methodName, args[0]);

        switch (methodName) {
            case "getMap":
                return map;
            case "getType":
                return clazz;
            case "assoc":
                return assoc((String)args[0], args[1]);
            case "assocEx":
                return assocEx((String)args[0], args[1]);
            case "without":
                return without((String)args[0]);
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

    private Object getValueFor(Method method) {
        String methodName = method.getName();
        Keyword keywordKey = Keyword.intern(methodName);
        IMapEntry entry = map.entryAt(keywordKey);
        if (entry == null)
            entry = getNonDefaultKey(method);
        if (entry == null)
            return null;
        Object val = entry.val();
        Class<?> returnType = method.getReturnType();
        if (returnType.equals(int.class) || returnType.equals(Integer.class))
            return ((Long) val).intValue();
        if (returnType.equals(float.class) || returnType.equals(Float.class))
            return ((Double) val).floatValue();
        if (returnType.equals(short.class) || returnType.equals(Short.class))
            return ((Long) val).shortValue();
        if (DynamicObject.class.isAssignableFrom(returnType))
            return DynamicObject.wrap((IPersistentMap) map.valAt(Keyword.intern(methodName)), returnType);
        return val;
    }

    private IMapEntry getNonDefaultKey(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation.annotationType().equals(Key.class)) {
                String key = ((Key) annotation).value();
                if (key.charAt(0) == ':')
                    key = key.substring(1);
                return map.entryAt(Keyword.intern(key));
            }
        }
        return null;
    }
}
