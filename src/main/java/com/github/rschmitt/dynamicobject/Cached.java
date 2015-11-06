package com.github.rschmitt.dynamicobject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requests that DynamicObject serializers with support for caching or deduplicating repeated values cache the value
 * stored under the associated getter or builder. Currently, only the Fressian serializer makes use of this annotation.
 * <p/>
 * This annotation does not impact binary compatibility of the serialized data.
 *
 * @since 1.6.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cached {
}