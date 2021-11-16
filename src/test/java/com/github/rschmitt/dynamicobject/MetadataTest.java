package com.github.rschmitt.dynamicobject;

import org.junit.jupiter.api.Test;

public class MetadataTest {
    @Test
    public void repro() {
        AnnotatedData AnnotatedData = DynamicObject.deserialize("{:value \"regular data\"}", AnnotatedData.class);
    }

    public interface AnnotatedData extends DynamicObject<AnnotatedData> {
        @Meta String source();
        AnnotatedData source(String meta);
        @Meta @Key(":source") AnnotatedData withSource(String meta);
    }
}
