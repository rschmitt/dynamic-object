package com.github.rschmitt.dynamicobject;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

public class MethodHandleTest {
    private static final UUID ReceiptHandle = UUID.randomUUID();

    @Test
    public void buildPolymorphically() {
        String edn = "{:command \"start the reactor\"}";

        QueueMessage queueMessage = deserializeAndAttachMetadata(edn, QueueMessage::receiptHandle, QueueMessage.class);

        assertEquals(ReceiptHandle, queueMessage.receiptHandle());
        assertEquals("start the reactor", queueMessage.command());
    }

    private <T> T deserializeAndAttachMetadata(String edn, BiFunction<T, UUID, T> receiptHandleMetadataBuilder, Class<T> type) {
        T instance = deserialize(edn, type);
        return receiptHandleMetadataBuilder.apply(instance, ReceiptHandle);
    }

    public interface QueueMessage extends DynamicObject<QueueMessage> {
        String command();

        @Meta UUID receiptHandle();
        QueueMessage receiptHandle(UUID receiptHandle);
    }
}
