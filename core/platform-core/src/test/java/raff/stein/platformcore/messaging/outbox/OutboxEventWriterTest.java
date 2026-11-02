package raff.stein.platformcore.messaging.outbox;

import io.cloudevents.CloudEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import raff.stein.platformcore.messaging.CloudEventExtensions;

import java.time.Clock;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OutboxEventWriterTest {

    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final OutboxRelay relay = mock(OutboxRelay.class);
    private final OutboxEventWriter writer = new OutboxEventWriter(
            repository, relay, Clock.fixed(OutboxTestEvents.NOW, ZoneOffset.UTC));

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldStoreTheWholeEventWithItsTopicAndExtensions() {
        final CloudEvent cloudEvent = OutboxTestEvents.cloudEvent("event-1", "BANK_A");

        writer.store("test-topic", cloudEvent);

        final ArgumentCaptor<OutboxEventEntity> saved = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(saved.capture());
        final OutboxEventEntity entity = saved.getValue();
        assertEquals("event-1", entity.getEventId());
        assertEquals("test-topic", entity.getTopic());
        assertEquals(OutboxStatus.PENDING, entity.getStatus());
        assertEquals(0, entity.getAttempts());
        assertEquals(OutboxTestEvents.NOW, entity.getNextAttemptAt());
        // the relay has no security context: the tenant must survive inside the stored event
        final CloudEvent restored = OutboxPayloads.deserialize(entity.getPayload());
        assertEquals("BANK_A", restored.getExtension(CloudEventExtensions.TENANT_ID));
        assertEquals("event-1", restored.getId());
        assertEquals("{\"value\":\"hello\"}", new String(restored.getData().toBytes()));
    }

    @Test
    void shouldWakeTheRelayOnlyAfterTheTransactionCommits() {
        TransactionSynchronizationManager.initSynchronization();

        writer.store("test-topic", OutboxTestEvents.cloudEvent("event-1", "BANK_A"));

        // the row isn't visible to the relay before the commit
        verify(relay, never()).wakeUp();
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(relay).wakeUp();
    }

    @Test
    void shouldNotWakeTheRelayWhenTheTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();

        writer.store("test-topic", OutboxTestEvents.cloudEvent("event-1", "BANK_A"));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(relay);
    }

    @Test
    void shouldRejectMissingTopic() {
        assertThrows(NullPointerException.class,
                () -> writer.store(null, OutboxTestEvents.cloudEvent("event-1", "BANK_A")));
    }
}
