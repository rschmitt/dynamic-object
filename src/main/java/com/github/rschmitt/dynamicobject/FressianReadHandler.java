package com.github.rschmitt.dynamicobject;

import org.fressian.Reader;
import org.fressian.handlers.ReadHandler;

import java.io.IOException;
import java.util.List;

import static com.github.rschmitt.dynamicobject.ClojureStuff.*;

class FressianReadHandler implements ReadHandler {
    private final Class type;
    private final String tag;

    FressianReadHandler(Class type, String tag) {
        this.type = type;
        this.tag = tag;
    }

    @Override
    public Object read(Reader r, Object tag, int componentCount) throws IOException {
        Object result = Transient.invoke(EmptyMap);
        List kvs = (List) r.readObject();
        for (int i = 0; i < kvs.size(); i += 2) {
            result = AssocBang.invoke(result, kvs.get(i), kvs.get(i + 1));
        }
        return DynamicObject.wrap(Persistent.invoke(result), type);
    }
}
