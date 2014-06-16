package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MetadataTest {
    private static final AnnotatedData ANNOTATED_DATA = DynamicObject.deserialize("{:value \"regular data\"}", AnnotatedData.class);

    @Test
    public void noInitialMetadata() {
        assertNull(ANNOTATED_DATA.source());
        assertEquals("{:value \"regular data\"}", DynamicObject.serialize(ANNOTATED_DATA));
    }

    @Test
    public void metadataBuilders() {
        AnnotatedData annotatedData = ANNOTATED_DATA.source("SQS");
        assertEquals("SQS", annotatedData.source());
    }

    @Test
    public void metadataIsNotSerialized() {
        AnnotatedData annotatedData = ANNOTATED_DATA.source("DynamoDB");
        assertEquals("{:value \"regular data\"}", DynamicObject.serialize(annotatedData));
    }

    @Test
    public void metadataIsIgnoredForEquality() {
        AnnotatedData withMetadata = ANNOTATED_DATA.source("Datomic");
        assertEquals(ANNOTATED_DATA, withMetadata);
    }
}

interface AnnotatedData extends DynamicObject<AnnotatedData> {
    @Meta String source();
    AnnotatedData source(String meta);
}