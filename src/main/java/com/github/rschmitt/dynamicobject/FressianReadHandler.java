package com.github.rschmitt.dynamicobject;

import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.AssocBang;
import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.EmptyMap;
import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.Persistent;
import static com.github.rschmitt.dynamicobject.internal.ClojureStuff.Transient;

import java.io.IOException;
import java.util.List;

import org.fressian.Reader;
import org.fressian.handlers.ReadHandler;

public class FressianReadHandler implements ReadHandler {
    private final Class type;

    public FressianReadHandler(Class type) {
        this.type = type;
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
