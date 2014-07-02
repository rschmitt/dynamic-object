package com.github.rschmitt.dynamicobject;

import org.junit.Test;

import java.util.UUID;
import java.util.function.BiFunction;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static org.junit.Assert.assertEquals;

public class MethodHandleTest {
    private static final UUID RECEIPT_HANDLE = UUID.randomUUID();

    @Test
    public void buildPolymorphically() {
        String edn = "{:command \"start the reactor\"}";

        QueueMessage queueMessage = deserializeAndAttachMetadata(edn, QueueMessage::receiptHandle, QueueMessage.class);

        assertEquals(RECEIPT_HANDLE, queueMessage.receiptHandle());
        assertEquals("start the reactor", queueMessage.command());
    }

    private <T> T deserializeAndAttachMetadata(String edn, BiFunction<T, UUID, T> receiptHandleMetadataBuilder, Class<T> type) {
        T instance = deserialize(edn, type);
        return receiptHandleMetadataBuilder.apply(instance, RECEIPT_HANDLE);
    }
}

interface QueueMessage extends DynamicObject<QueueMessage> {
    String command();

    @Meta UUID receiptHandle();
    QueueMessage receiptHandle(UUID receiptHandle);
}