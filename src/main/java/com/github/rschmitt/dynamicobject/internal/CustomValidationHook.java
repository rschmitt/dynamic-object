package com.github.rschmitt.dynamicobject.internal;

import com.github.rschmitt.dynamicobject.DynamicObject;

public interface CustomValidationHook<T extends DynamicObject<T>> {
    T $$customValidate();
}
