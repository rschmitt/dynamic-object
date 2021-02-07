package com.github.rschmitt.dynamicobject.internal.indyproxy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;

class ConcreteMethodTracker {
    private HashSet<Method> contributors = new HashSet<>();

    public void add(Method m) {
        if ((m.getModifiers() & Modifier.ABSTRACT) != 0) return;

        // Remove any concrete implementations that come from superclasses of the class that owns this new method
        // (this new class shadows them).
        contributors.removeIf(m2 -> m2.getDeclaringClass().isAssignableFrom(m.getDeclaringClass()));

        // Conversely, if this new implementation is shadowed by any existing implementations, we'll drop it instead
        if (contributors.stream().anyMatch(m2 -> m.getDeclaringClass().isAssignableFrom(m2.getDeclaringClass()))) {
            return;
        }

        contributors.add(m);
    }

    public Method getOnlyContributor() {
        if (contributors.size() != 1) return null;

        return contributors.iterator().next();
    }
}
