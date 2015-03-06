package com.github.rschmitt.dynamicobject;

import clojure.java.api.Clojure;
import org.fressian.FressianReader;
import org.fressian.FressianWriter;
import org.fressian.Reader;
import org.fressian.Writer;
import org.fressian.handlers.ILookup;
import org.fressian.handlers.ReadHandler;
import org.fressian.handlers.WriteHandler;
import org.fressian.impl.Handlers;
import org.fressian.impl.InheritanceLookup;
import org.fressian.impl.MapLookup;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.rschmitt.dynamicobject.ClojureStuff.*;
import static org.junit.Assert.assertEquals;

public class FressianTest {
    private static final String TAG = "user/BinarySerialized";

    @Test
    public void proofOfConcept() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(250000);
        BinarySerialized binarySerialized = DynamicObject.newInstance(BinarySerialized.class).withHello("world");

        HashMap<Class, Map<String, WriteHandler>> userWriteHandlers = new HashMap<>();
        Handlers.installHandler(userWriteHandlers, BinarySerialized.class, TAG, new DynamicObjectWriter());
        ILookup<Class, Map<String, WriteHandler>> classMapMapLookup = new InheritanceLookup<>(new MapLookup<>(userWriteHandlers));

        FressianWriter fressianWriter = new FressianWriter(baos, classMapMapLookup);
        fressianWriter.writeObject(binarySerialized);
        fressianWriter.writeObject(binarySerialized);
        fressianWriter.writeFooter();
        fressianWriter.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Map<Object, ReadHandler> userReadHandlers = new HashMap<>();
        userReadHandlers.put(TAG, new DynamicObjectReader());
        FressianReader fressianReader = new FressianReader(bais, new MapLookup<>(userReadHandlers));

        assertEquals(binarySerialized, fressianReader.readObject());
        assertEquals(binarySerialized, fressianReader.readObject());
        fressianReader.validateFooter();
        fressianReader.close();
    }

    public interface BinarySerialized extends DynamicObject<BinarySerialized> {
        @Key("hello")
        String hello();

        @Key("hello")
        BinarySerialized withHello(String hello);
    }

    public static class DynamicObjectReader implements ReadHandler {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            Object result = Transient.invoke(Clojure.read("{}"));
            List kvs = (List) r.readObject();
            for (int i = 0; i < kvs.size(); i += 2) {
                result = AssocBang.invoke(result, kvs.get(i), kvs.get(i + 1));
            }
            return DynamicObject.wrap(Persistent.invoke(result), BinarySerialized.class);
        }
    }

    public static class DynamicObjectWriter implements WriteHandler {
        @Override
        @SuppressWarnings("unchecked")
        public void write(Writer w, Object instance) throws IOException {
            w.writeTag(TAG, 1);
            Map<Object, Object> map = ((DynamicObject) instance).getMap();
            List<Object> associativeList = new ArrayList<>(2 * map.size());
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                associativeList.add(entry.getKey());
                associativeList.add(entry.getValue());
            }
            w.writeList(associativeList);
        }
    }
}
