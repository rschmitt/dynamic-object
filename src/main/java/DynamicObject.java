import clojure.lang.*;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public interface DynamicObject<T> {
    /**
     * @return the underlying IPersistentMap backing this instance.
     */
    IPersistentMap getMap();

    /**
     * @return the Class for this instance's type. This method is required due to erasure.
     */
    Class<T> getType();

    /**
     * Return a persistent copy of this object with the new value associated with the given key.
     */
    default <T> T assoc(String key, Object value) {
        Keyword keyword = Keyword.intern(Symbol.intern(key));
        IPersistentMap newMap = getMap().assoc(keyword, value);
        return wrap(newMap, getType());
    }

    /**
     * Deserializes a DynamicObject from a String.
     * @param edn The String representation of the object, serialized in Edn, Clojure's Extensible Data Notation.
     * @param clazz The type of class to deserialize. Must be an interface that extends DynamicObject.
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(String edn, Class clazz) {
        IPersistentMap map = (IPersistentMap) EdnReader.readString(edn, PersistentHashMap.EMPTY);
        return wrap(map, clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> T wrap(IPersistentMap map, Class clazz) {
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{clazz},
                (Object proxy, Method method, Object[] args) -> {
                    String methodName = method.getName();

                    switch (methodName) {
                        case "getMap":
                            return map;
                        case "getType":
                            return clazz;
                        case "assoc":
                            return MethodHandles.lookup()
                                    .in(method.getDeclaringClass())
                                    .unreflectSpecial(method, method.getDeclaringClass())
                                    .bindTo(proxy)
                                    .invokeWithArguments(args);
                        case "toString":
                        case "hashCode":
                            return method.invoke(map, args);
                        case "equals":
                            Object other = args[0];
                            if (other instanceof DynamicObject) {
                                return map.equals(((DynamicObject) other).getMap());
                            } else
                                return method.invoke(map, args);
                        default:
                            Keyword keywordKey = Keyword.intern(Symbol.intern(methodName));
                            Object val = map.entryAt(keywordKey).val();
                            Class<?> returnType = method.getReturnType();
                            if (returnType.equals(int.class) || returnType.equals(Integer.class))
                                return ((Long) val).intValue();
                            if (returnType.equals(float.class) || returnType.equals(Float.class))
                                return ((Double) val).floatValue();
                            if (returnType.equals(short.class) || returnType.equals(Short.class))
                                return ((Long) val).shortValue();
                            if (DynamicObject.class.isAssignableFrom(returnType))
                                return DynamicObject.wrap((IPersistentMap) map.valAt(Keyword.intern(Symbol.intern(methodName))), returnType);
                            return val;
                    }
                }
        );
    }
}
