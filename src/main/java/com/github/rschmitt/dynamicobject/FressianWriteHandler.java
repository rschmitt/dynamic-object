package com.github.rschmitt.dynamicobject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        Map<Object, Object> map = ((DynamicObject) instance).getMap();
        List<Object> kvs = new ArrayList<>(2 * map.size());
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            kvs.add(entry.getKey());
            kvs.add(entry.getValue());
        }
        w.writeList(kvs);
    }
}
