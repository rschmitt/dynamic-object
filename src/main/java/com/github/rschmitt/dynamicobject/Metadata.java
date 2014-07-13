package com.github.rschmitt.dynamicobject;

import static com.github.rschmitt.dynamicobject.ClojureStuff.*;

public class Metadata {
    static Class<?> getTypeMetadata(Object obj) {
        Object meta = Meta.invoke(obj);
        if (meta == null) return null;
        Object tag = Get.invoke(meta, Type);
        if (tag == null) return null;
        try {
            return Class.forName((String) Name.invoke(tag));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static Object withTypeMetadata(Object obj, Class<?> type) {
        return VaryMeta.invoke(obj, Assoc, Type, cachedRead(":" + type.getTypeName()));
    }
}
