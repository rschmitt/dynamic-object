package com.github.rschmitt.dynamicobject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requests that DynamicObject serializers with support for caching or deduplicating repeated values cache the value
 * stored under the associated getter or builder. Currently, only the Fressian serializer makes use of this annotation.
 * <p>
 * The objects stored under this annotation should implement a sensible equals() and hashCode(); additionally, when
 * using @Cached, you should be cognizant that the cache used by the serializer may be quite small - Fressian has only
 * 32 entries, for instance, and this is shared with cached map keys as well. Therefore, using @Cached on data that
 * cannot be effectively cached can greatly negatively impact the size of the encoded data. In particular, using @Cached
 * on data that uses default object identity comparison may be a very bad idea.
 * <p>
 * This annotation does not impact binary compatibility of the serialized data.
 *
 * @since 1.6.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cached {
}
