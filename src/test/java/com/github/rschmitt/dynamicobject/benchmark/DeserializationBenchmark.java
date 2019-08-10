package com.github.rschmitt.dynamicobject.benchmark;

import com.github.rschmitt.dynamicobject.DynamicObject;
import com.github.rschmitt.dynamicobject.Key;
import org.fressian.FressianWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DeserializationBenchmark {
    private final int ITERATIONS = 1_000_000;
    private final int NUM_THREADS = 8;

    @Test
    public void run() throws Exception {
        DynamicObject.registerTag(StringFieldList.class, "string-field-list-tag");
        DynamicObject.registerTag(StringField.class, "string-field-tag");
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<Long>> futures = new ArrayList<>();

        long acc = 0;
        long startTime = System.nanoTime();
        for (int i = 0; i < NUM_THREADS; i++) {
            futures.add(executorService.submit(this::perfTest));
        }

        for (Future<Long> future : futures) {
            acc += future.get();
        }
        long endTime = System.nanoTime();

        System.out.println("Total bytes deserialized (MiB):" + acc / 1024.0 / 1024.0);
        reportTime("stringField", startTime, endTime);
    }

    private long perfTest() {
        StringFieldList stringFieldList = getStringFieldList();
        byte[] buffer = serialize(stringFieldList);

        System.out.println("Serialization size (B) = " + buffer.length);
        long bytesDeserialized = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            deserialize(buffer);
            bytesDeserialized += buffer.length;
        }
        return bytesDeserialized;
    }

    private void reportTime(String desc, long startTime, long endTime) {
        long timeInMillis = (endTime - startTime) / 1000000;
        System.out.println(String.format("%s: %,d ms", desc, timeInMillis));
    }

    private StringFieldList deserialize(byte[] buffer) {
        try {
            return (StringFieldList) DynamicObject.createFressianReader(new ByteArrayInputStream(buffer), false).readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] serialize(StringFieldList stringFieldList) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FressianWriter writer = DynamicObject.createFressianWriter(baos);
        try {
            writer.writeObject(stringFieldList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    private StringFieldList getStringFieldList() {
        List<StringField> stringFields = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            stringFields.add(DynamicObject.newInstance(StringField.class).withString("str"));
        }
        return DynamicObject.newInstance(StringFieldList.class)
                .withStringFields(stringFields);
    }

    public interface StringFieldList extends DynamicObject<StringFieldList> {
        @Key(":string-fields") StringField getStringFields();
        @Key(":string-fields") StringFieldList withStringFields(List<StringField> stringList);
    }

    public interface StringField extends DynamicObject<StringField> {
        @Key(":string-field") String getString();
        @Key(":string-field") StringField withString(String string);
    }

}
