package com.github.rschmitt.dynamicobject.internal;

import com.github.rschmitt.dynamicobject.DynamicObject;

public interface CustomValidationHook<D extends DynamicObject<D>> {
    D $$customValidate();
}
