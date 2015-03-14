package com.github.rschmitt.dynamicobject.benchmark;

import com.github.rschmitt.dynamicobject.DynamicObject;
import com.github.rschmitt.dynamicobject.DynamicObjects;
import org.junit.Test;

import java.util.Random;

public class DefaultMethodInvocation {
    public static long sum = 0;
    public static final Random R = new Random();

    @Test
    public void benchmarkDefaultMethodInvocation() {
        DynamicObjects.USE_INVOKEDYNAMIC = false;
        sideEffectBenchmark();

        DynamicObjects.USE_INVOKEDYNAMIC = true;
        sideEffectBenchmark();
    }

    private static void sideEffectBenchmark() {
        SideEffecter ef = DynamicObject.newInstance(SideEffecter.class);

        for (int i = 0; i < 15_000_000; i++)
            ef.pushBack();

        long startTime = System.nanoTime();
        for (int i = 0; i < 50_000_000; i++)
            ef.pushBack();
        long endTime = System.nanoTime();

        System.out.printf("Sum was %,d%n", sum);
        double time = ((double) (endTime - startTime)) / 1_000_000_000D;
        System.out.printf("%,3.2f seconds to run; USE_INVOKEDYNAMIC = %s%n", time, DynamicObjects.USE_INVOKEDYNAMIC);
    }

    public interface SideEffecter extends DynamicObject<SideEffecter> {
        default void pushBack() {
            sum += R.nextInt(1 << 8);
        }
    }

}
