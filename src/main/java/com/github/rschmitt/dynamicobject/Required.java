package com.github.rschmitt.dynamicobject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as required. Fields marked with this annotation will throw a NullPointerException when accessed if a
 * nonnull value is not present, or when the validate() method is called on the instance.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Required {
}
