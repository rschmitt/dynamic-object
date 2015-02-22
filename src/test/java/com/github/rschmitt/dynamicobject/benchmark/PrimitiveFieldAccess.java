package com.github.rschmitt.dynamicobject.benchmark;

import com.github.rschmitt.dynamicobject.DynamicObject;
import org.junit.Test;

public class PrimitiveFieldAccess {
    private final int Iterations = 500_000;

    @Test
    public void run() {
        IntPojo intPojo = new IntPojo(1);
        FinalIntPojo finalIntPojo = new FinalIntPojo(1);
        IntField intField = DynamicObject.newInstance(IntField.class).i(1);

        int acc = 0;
        for (int i = 0; i < 15_000; i++) {
            acc += intPojo.getI();
            acc += finalIntPojo.getI();
            acc += intField.i();
        }
        System.out.println(acc);

        acc = 0;
        long startTime = System.nanoTime();
        for (int i = 0; i < Iterations; i++) {
            acc += intPojo.getI();
        }
        long endTime = System.nanoTime();
        System.out.println(acc);
        reportTime("intPojo", startTime, endTime);


        acc = 0;
        startTime = System.nanoTime();
        for (int i = 0; i < Iterations; i++) {
            acc += finalIntPojo.getI();
        }
        endTime = System.nanoTime();
        System.out.println(acc);
        reportTime("finalIntPojo", startTime, endTime);


        acc = 0;
        startTime = System.nanoTime();
        for (int i = 0; i < Iterations; i++) {
            acc += intField.i();
        }
        endTime = System.nanoTime();
        System.out.println(acc);
        reportTime("intField", startTime, endTime);
    }

    private void reportTime(String desc, long startTime, long endTime) {
        long timeInMillis = (endTime - startTime) / 1000000;
        System.out.println(String.format("%s: %,d ms", desc, timeInMillis));
    }
}


interface IntField extends DynamicObject<IntField> {
    int i();

    IntField i(int i);
}

class IntPojo {
    int i;

    IntPojo(int i) {
        this.i = i;
    }

    int getI() {
        return i;
    }
}

class FinalIntPojo {
    private final int i;

    FinalIntPojo(int i) {
        this.i = i;
    }

    int getI() {
        return i;
    }
}
