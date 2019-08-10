package com.github.rschmitt.dynamicobject;

import org.fressian.Reader;
import org.fressian.handlers.ReadHandler;

import java.io.IOException;
import java.util.Map;

public class FressianReadHandler<D extends DynamicObject<D>> implements ReadHandler {
    private final Class<D> type;

    public FressianReadHandler(Class<D> type) {
        this.type = type;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Object read(Reader r, Object tag, int componentCount) throws IOException {
        return DynamicObject.wrap((Map) r.readObject(), type).afterDeserialization();
    }
}
