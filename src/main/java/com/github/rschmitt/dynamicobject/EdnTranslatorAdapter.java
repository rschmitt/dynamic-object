package com.github.rschmitt.dynamicobject;

import clojure.lang.AFn;

import java.io.IOException;
import java.io.Writer;

final class EdnTranslatorAdapter<T> extends AFn {
    private final EdnTranslator<T> ednTranslator;

    EdnTranslatorAdapter(EdnTranslator<T> ednTranslator) {
        this.ednTranslator = ednTranslator;
    }

    @Override
    public Object invoke(Object arg) {
        return ednTranslator.read(arg);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object arg1, Object arg2) {
        Writer writer = (Writer) arg2;
        try {
            writer.write(String.format("#%s", ednTranslator.getTag()));
            ednTranslator.write((T) arg1, writer);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return null;
    }

    public final String getTag() {
        return ednTranslator.getTag();
    }
}
