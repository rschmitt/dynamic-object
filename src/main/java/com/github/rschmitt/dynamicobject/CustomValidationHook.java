package com.github.rschmitt.dynamicobject;

public interface CustomValidationHook<T extends DynamicObject<T>> {
    T $$customValidate();
}
