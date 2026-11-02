package raff.stein.platformcore.messaging.outbox;

import io.cloudevents.CloudEvent;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import raff.stein.platformcore.messaging.CloudEventExtensions;
import raff.stein.platformcore.messaging.publisher.WMPBaseEventPublisher;
import raff.stein.platformcore.messaging.publisher.model.EventData;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OutboxPublisherTest {

    public record SamplePayload(String value) {
    }

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private Producer<String, CloudEvent> producer;
    private WMPBaseEventPublisher publisher;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        producer = mock(Producer.class);
        publisher = new WMPBaseEventPublisher(producer, objectMapper, "test-service");
        SecurityContextHolder.setContext(WMPContext.builder().userId("user-1").tenantId("BANK_A").build());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    void shouldStoreInTheOutboxInsteadOfSendingWhenEnabled() {
        final OutboxEventWriter writer = mock(OutboxEventWriter.class);
        publisher.setOutboxEventWriter(writer);

        publisher.publishCloudEvent("test-topic", new EventData(new SamplePayload("hello")));

        final ArgumentCaptor<CloudEvent> stored = ArgumentCaptor.forClass(CloudEvent.class);
        verify(writer).store(org.mockito.ArgumentMatchers.eq("test-topic"), stored.capture());
        // the security context is read now, in the caller's thread: the relay won't have one
        assertEquals("BANK_A", stored.getValue().getExtension(CloudEventExtensions.TENANT_ID));
        assertEquals("user-1", stored.getValue().getExtension(CloudEventExtensions.USER_ID));
        verifyNoInteractions(producer);
    }

    @Test
    void shouldSendStraightToKafkaWhenTheOutboxIsOffAndThereIsNoTransaction() {
        publisher.publishCloudEvent("test-topic", new EventData(new SamplePayload("hello")));

        verify(producer).send(any(ProducerRecord.class), any());
    }

    @Test
    void shouldHoldTheEventUntilTheCommitWhenTheOutboxIsOffInsideATransaction() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            publisher.publishCloudEvent("test-topic", new EventData(new SamplePayload("hello")));

            verifyNoInteractions(producer);
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(producer).send(any(ProducerRecord.class), any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
