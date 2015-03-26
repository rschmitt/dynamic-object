package com.github.rschmitt.dynamicobject;

import java.io.IOException;

import org.fressian.Writer;
import org.fressian.handlers.WriteHandler;

public class FressianWriteHandler implements WriteHandler {
    private final Class type;
    private final String tag;

    public FressianWriteHandler(Class type, String tag) {
        this.type = type;
        this.tag = tag;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(Writer w, Object instance) throws IOException {
        w.writeTag(tag, 1);
        w.writeObject(((DynamicObject) instance).getMap());
    }
}
