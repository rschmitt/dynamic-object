package com.github.rschmitt.dynamicobject.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;

import clojure.java.api.Clojure;
import com.github.rschmitt.dynamicobject.Cached;
import com.github.rschmitt.dynamicobject.DynamicObject;
import com.github.rschmitt.dynamicobject.Key;
import com.github.rschmitt.dynamicobject.Meta;
import com.github.rschmitt.dynamicobject.Required;

public class ReflectionTest {
    @Test
    public void fieldGetters() throws Exception {
        Collection<Method> methods = Reflection.fieldGetters(VariousMethods.class);

        assertEquals(2, methods.size());
        assertTrue(methods.contains(VariousMethods.class.getMethod("num")));
        assertTrue(methods.contains(VariousMethods.class.getMethod("customKey")));
    }

    @Test
    public void requiredFields() throws Exception {
        Collection<Method> methods = Reflection.requiredFields(VariousMethods.class);

        assertEquals(1, methods.size());
        assertTrue(methods.contains(VariousMethods.class.getMethod("num")));
    }

    @Test
    public void testCachedAnnotations() throws Exception {
        HashSet<Object> expectedKeys = new HashSet<>(Arrays.asList(
                Clojure.read(":cachedGetter"),
                Clojure.read(":cachedGetterWithKey"),
                Clojure.read(":cachedBuilder"),
                Clojure.read(":cachedBuilderWithKey"),
                Clojure.read(":nameFromGetter")
        ));

        HashSet<Object> actualKeys = new HashSet<>(Reflection.cachedKeys(CachedAnnotationTests.class));

        assertEquals(expectedKeys, actualKeys);
    }

    public interface CachedAnnotationTests extends DynamicObject<CachedAnnotationTests> {
        @Cached Object cachedGetter();
        @Key(":cachedGetterWithKey") @Cached Object differentMethodName();

        Object cachedBuilder();
        @Cached CachedAnnotationTests cachedBuilder(Object value);
        @Key(":cachedBuilderWithKey") @Cached CachedAnnotationTests differentMethodName2(Object value);

        @Key(":nameFromGetter") Object nameFromGetterFunc();
        @Cached CachedAnnotationTests nameFromGetterFunc(Object value);
    }
}

interface VariousMethods extends DynamicObject<VariousMethods> {
    @Required int num();
    @Key(":custom-key") String customKey();
    @Meta String someMetadata();

    VariousMethods num(int num);

    default void customMethod() {

    }

    default boolean anotherCustomMethod(int x) {
        return true;
    }
}
