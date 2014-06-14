package com.github.rschmitt.dynamicobject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static com.github.rschmitt.dynamicobject.ClojureStuff.*;

/*
 * This class contains functions responsible for taking collections, identifying unwrapped DynamicObject maps within
 * those collections, and wrapping them as proxies according to their :type metadata.
 */
class Reification {
    @SuppressWarnings("unchecked")
    static Object wrapElements(Object coll, Object empty, Type genericReturnType) {
        Class<?> typeParameter = (Class<?>) ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
        long count = (int) COUNT.invoke(coll);
        Object ret = TRANSIENT.invoke(empty);
        Object head = FIRST.invoke(coll);
        coll = REST.invoke(coll);
        for (int i = 0; i < count; i++) {
            head = Numerics.maybeDownconvert(typeParameter, head);
            CONJ_BANG.invoke(ret, maybeWrapElement(head));
            head = FIRST.invoke(coll);
            coll = REST.invoke(coll);
        }
        return PERSISTENT.invoke(ret);
    }

    static Object wrapMapElements(Object unwrappedMap, Type genericReturnType) {
        Type[] actualTypeArguments = ((ParameterizedType) genericReturnType).getActualTypeArguments();
        Class<?> keyType = (Class<?>) actualTypeArguments[0];
        Class<?> valType = (Class<?>) actualTypeArguments[1];
        Object ret = EMPTY_MAP;
        ret = TRANSIENT.invoke(ret);
        Object head = FIRST.invoke(unwrappedMap);
        unwrappedMap = REST.invoke(unwrappedMap);
        while (head != null) {
            Object key = KEY.invoke(head);
            Object val = VAL.invoke(head);
            key = maybeWrapElement(key);
            val = maybeWrapElement(val);
            key = Numerics.maybeDownconvert(keyType, key);
            val = Numerics.maybeDownconvert(valType, val);
            ASSOC_BANG.invoke(ret, key, val);

            head = FIRST.invoke(unwrappedMap);
            unwrappedMap = REST.invoke(unwrappedMap);
        }
        return PERSISTENT.invoke(ret);
    }

    @SuppressWarnings("unchecked")
    private static Object maybeWrapElement(Object obj) {
        Class<?> type = getTypeFromMetadata(obj);
        if (type == null)
            return obj;
        else
            return DynamicObject.wrap(obj, (Class<DynamicObject>) type);
    }

    private static Class<?> getTypeFromMetadata(Object obj) {
        String canonicalName = getTypeMetadata(obj);
        if (canonicalName == null) return null;
        try {
            return Class.forName(canonicalName);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String getTypeMetadata(Object obj) {
        Object metadata = META.invoke(obj);
        if (metadata == null) return null;
        Object typeMetadata = GET.invoke(metadata, TYPE);
        return (String) NAME.invoke(typeMetadata);
    }
}
