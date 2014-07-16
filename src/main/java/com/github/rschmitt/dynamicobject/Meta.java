package com.github.rschmitt.dynamicobject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a field as metadata. Metadata is used to annotate a value with additional information that is not logically
 * considered part of the value. Metadata is ignored for the purposes of equality and serialization.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Meta {
}
