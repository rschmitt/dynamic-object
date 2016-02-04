package com.github.rschmitt.dynamicobject;

import lombok.RequiredArgsConstructor;
import org.fressian.CachedObject;
import org.fressian.Writer;
import org.fressian.handlers.WriteHandler;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FressianWriteHandler<D extends DynamicObject<D>> implements WriteHandler {
    private final Class<D> type;
    private final String tag;
    private final Set<Object> cachedKeys;

    public FressianWriteHandler(Class<D> type, String tag, Set<Object> cachedKeys) {
        this.type = type;
        this.tag = tag;
        this.cachedKeys = cachedKeys;
    }

    @Override
    public void write(Writer w, Object instance) throws IOException {
        // We manually serialize the backing map so that we can apply caching transformations to specific subcomponents.
        // To avoid needless copying we do this via an adapter rather than copying to a temporary list.
        w.writeTag(tag, 1);
        w.writeTag("map", 1);

        Map map = ((DynamicObject) instance).getMap();
        w.writeList(new TransformedMap(map, this::transformKey, this::transformValue));
    }

    /*
     * Although Fressian will automatically cache the string components of each Keyword, by default we still spend a
     * minimum of three bytes per keyword - one for the keyword directive itself, one for the namespace (usually null),
     * and one for the cached reference to the keyword's string. By requesting caching of the Keyword object itself like
     * this, we can get this down to one byte (after the initial cache miss).
     *
     * The downside of this is that cache misses incur additional an additional byte marking the key as being LRU-cache
     * capable, and the cache misses will also result in two cache entries being introduced, causing more cache churn
     * than there would be otherwise.
     */
    private Object transformKey(Object key) {
        return new CachedObject(key);
    }

    @SuppressWarnings("unchecked")
    private Object transformValue(Object key, Object value) {
        if (cachedKeys.contains(key)) {
            return new CachedObject(value);
        }

        return value;
    }

    @Immutable
    @RequiredArgsConstructor
    @SuppressWarnings("unchecked")
    private static class TransformedMap extends AbstractCollection {
        private final Map backingMap;
        private final Function<Object, Object> keysTransformation;
        private final BiFunction<Object, Object, Object> valuesTransformation;

        @Override
        public Iterator iterator() {
            return new TransformingKeyValueIterator(backingMap.entrySet().iterator(), keysTransformation, valuesTransformation);
        }

        @Override
        public int size() {
            return backingMap.size() * 2;
        }
    }

    @NotThreadSafe
    @RequiredArgsConstructor
    private static class TransformingKeyValueIterator implements Iterator {
        private final Iterator<Map.Entry> entryIterator;
        private final Function<Object, Object> keysTransformation;
        private final BiFunction<Object, Object, Object> valuesTransformation;

        Object pendingValue = null;
        boolean hasPendingValue = false;

        @Override
        public boolean hasNext() {
            return hasPendingValue || entryIterator.hasNext();
        }

        @Override
        public Object next() {
            if (hasPendingValue) {
                Object value = pendingValue;
                pendingValue = null;
                hasPendingValue = false;

                return value;
            }

            Map.Entry entry = entryIterator.next();
            pendingValue = valuesTransformation.apply(entry.getKey(), entry.getValue());

            hasPendingValue = true;

            return keysTransformation.apply(entry.getKey());
        }
    }
}
