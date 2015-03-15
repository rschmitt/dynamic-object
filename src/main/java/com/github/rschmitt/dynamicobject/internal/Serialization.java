package com.github.rschmitt.dynamicobject.internal;

import java.io.EOFException;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.github.rschmitt.dynamicobject.DynamicObject;

public class Serialization {
    @SuppressWarnings("unchecked")
    public static synchronized <T> void deregisterType(Class<T> type) {
        EdnSerialization.deregisterType(type);
        FressianSerialization.deregisterType(type);
    }

    public static synchronized <D extends DynamicObject<D>> void registerTag(Class<D> type, String tag) {
        EdnSerialization.registerTag(type, tag);
        FressianSerialization.registerTag(type, tag);
    }

    public static synchronized <D extends DynamicObject<D>> void deregisterTag(Class<D> type) {
        EdnSerialization.deregisterTag(type);
        FressianSerialization.deregisterTag(type);
    }

    @FunctionalInterface
    interface IOSupplier<T> {
        T get() throws IOException;
    }

    static <T> Iterator<T> deserializeStreamToIterator(IOSupplier<T> streamReader, Class<T> type) {
        return new Iterator<T>() {
            private T stash = null;
            private boolean done = false;

            @Override
            public boolean hasNext() {
                populateStash();
                return !done || stash != null;
            }

            @Override
            public T next() {
                if (hasNext()) {
                    T ret = stash;
                    stash = null;
                    return ret;
                } else
                    throw new NoSuchElementException();
            }

            private void populateStash() {
                if (stash != null || done)
                    return;
                try {
                    stash = streamReader.get();
                } catch (NoSuchElementException | EOFException ignore) {
                    done = true;
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }
}
