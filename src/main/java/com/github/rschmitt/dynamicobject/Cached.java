package com.github.rschmitt.dynamicobject;

import java.lang.annotation.*;

/**
 * Requests that DynamicObject serializers with support for caching or deduplicating repeated values cache the value
 * stored under the associated getter or builder. Currently, only the Fressian serializer makes use of this annotation.
 * <br>
 * Yhis annotation does not impact binary compatibility of the serialized data.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cached {
}
