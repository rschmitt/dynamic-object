package com.github.rschmitt.dynamicobject.benchmark;

import org.junit.jupiter.api.Test;

import com.github.rschmitt.dynamicobject.DynamicObject;

public class StringFieldAccess {
    private final int Iterations = 500_000;

    @Test
    public void run() {
        StringPojo stringPojo = new StringPojo("str");
        FinalStringPojo finalStringPojo = new FinalStringPojo("str");
        StringField stringField = DynamicObject.newInstance(StringField.class).str("str");

        int acc = 0;
        for (int i = 0; i < 15_000; i++) {
            acc += stringPojo.str().length();
            acc += finalStringPojo.str().length();
            acc += stringField.str().length();
        }

        acc = 0;
        long startTime = System.nanoTime();
        for (int i = 0; i < Iterations; i++) {
            acc += stringPojo.str().length();
        }
        long endTime = System.nanoTime();
        System.out.println(acc);
        reportTime("stringPojo", startTime, endTime);


        acc = 0;
        startTime = System.nanoTime();
        for (int i = 0; i < Iterations; i++) {
            acc += finalStringPojo.str().length();
        }
        endTime = System.nanoTime();
        System.out.println(acc);
        reportTime("finalStringPojo", startTime, endTime);


        acc = 0;
        startTime = System.nanoTime();
        for (int i = 0; i < Iterations; i++) {
            acc += stringField.str().length();
        }
        endTime = System.nanoTime();
        System.out.println(acc);
        reportTime("stringField", startTime, endTime);
    }

    private void reportTime(String desc, long startTime, long endTime) {
        long timeInMillis = (endTime - startTime) / 1000000;
        System.out.println(String.format("%s: %,d ms", desc, timeInMillis));
    }

    public interface StringField extends DynamicObject<StringField> {
        String str();

        StringField str(String str);
    }
}

class StringPojo {
    String str;

    StringPojo(String str) {
        this.str = str;
    }

    String str() {
        return str;
    }
}

class FinalStringPojo {
    private final String str;

    FinalStringPojo(String str) {
        this.str = str;
    }

    String str() {
        return str;
    }
}
