package com.github.rschmitt.dynamicobject;

import lombok.RequiredArgsConstructor;
import org.fressian.CachedObject;
import org.fressian.Writer;
import org.fressian.handlers.WriteHandler;

import java.io.IOException;
import java.util.*;
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
    @SuppressWarnings("unchecked")
    public void write(Writer w, Object instance) throws IOException {
        // We manually serialize the backing map so that we can apply caching transformations to specific subcomponents.
        // To avoid needless copying we do this via an adapter rather than copying to a temporary list.
        w.writeTag(tag, 1);
        w.writeTag("map", 1);

        Map map = ((DynamicObject)instance).getMap();
        w.writeList(new ExplodingCollection(map, this::transformKey, this::transformValue));
    }

    private Object transformKey(Object key) {
        // Although fressian will automatically cache the string components of each Keyword, by default we still
        // spend a minimum of three bytes per keyword - one for the keyword directive itself, one for the namespace
        // (usually null), and one for the cached reference to the keyword's string. By requesting caching of the
        // Keyword object itself like this, we can get this down to one byte (after the initial cache miss).

        // The downside of this is that cache misses incur additional an additional byte marking the key as
        // being LRU-cache capable, and the cache misses will also result in two cache entries being introduced,
        // causing more cache churn than there would be otherwise.

        return new CachedObject(key);
    }

    private Object transformValue(Object key, Object value) {
        if (cachedKeys.contains(key)) {
            return new CachedObject(value);
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    @RequiredArgsConstructor
    private static class ExplodingCollection extends AbstractCollection {
        final Map backingMap;
        final Function<Object, Object> keysTransformation; // k -> k
        final BiFunction<Object, Object, Object> valuesTransformation;

        @Override
        public Iterator iterator() {
            return new ExplodingIterator(backingMap.entrySet().iterator(), keysTransformation, valuesTransformation);
        }

        @Override
        public int size() {
            return backingMap.size() * 2;
        }
    }

    @RequiredArgsConstructor
    private static class ExplodingIterator implements Iterator {
        final Iterator<Map.Entry> entryIterator;
        final Function<Object, Object> keysTransformation; // k -> k
        final BiFunction<Object, Object, Object> valuesTransformation;

        Optional<Object> pendingValue = Optional.empty();

        @Override
        public boolean hasNext() {
            return pendingValue.isPresent() || entryIterator.hasNext();
        }

        @Override
        public Object next() {
            if (pendingValue.isPresent()) {
                Object value = pendingValue.get();
                pendingValue = Optional.empty();

                return value;
            }

            Map.Entry entry = entryIterator.next();
            pendingValue = Optional.of(valuesTransformation.apply(entry.getKey(), entry.getValue()));

            return keysTransformation.apply(entry.getKey());
        }
    }

}
