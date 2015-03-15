package com.github.rschmitt.dynamicobject;

import java.io.EOFException;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Serialization {
    @SuppressWarnings("unchecked")
    static synchronized <T> void deregisterType(Class<T> type) {
        EdnSerialization.deregisterType(type);
        FressianSerialization.deregisterType(type);
    }

    static synchronized <T extends DynamicObject<T>> void registerTag(Class<T> type, String tag) {
        EdnSerialization.registerTag(type, tag);
        FressianSerialization.registerTag(type, tag);
    }

    static synchronized <T extends DynamicObject<T>> void deregisterTag(Class<T> type) {
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
