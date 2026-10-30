package raff.stein.platformcore.messaging;

import io.cloudevents.CloudEvent;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import raff.stein.platformcore.messaging.consumer.WMPBaseEventConsumer;
import raff.stein.platformcore.messaging.publisher.WMPBaseEventPublisher;
import raff.stein.platformcore.messaging.publisher.model.EventData;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TenantEventPropagationTest {

    public record SamplePayload(String value) {
    }

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRestoreProducerTenantInConsumerContext() {
        final Producer<String, CloudEvent> producer = mock(Producer.class);
        final WMPBaseEventPublisher publisher = new WMPBaseEventPublisher(producer, objectMapper, "test-service");
        SecurityContextHolder.setContext(WMPContext.builder()
                .userId("user-1")
                .tenantId("BANK_A")
                .branchCode("BRANCH_01")
                .build());

        publisher.publishCloudEvent("test-topic", new EventData(new SamplePayload("hello")));

        final ArgumentCaptor<ProducerRecord<String, CloudEvent>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(sent.capture(), any());
        final CloudEvent cloudEvent = sent.getValue().value();
        assertEquals("BANK_A", cloudEvent.getExtension(CloudEventExtensions.TENANT_ID));
        assertEquals("BRANCH_01", cloudEvent.getExtension(CloudEventExtensions.BRANCH_CODE));

        // the consumer runs on another thread in real life: start from an empty context
        SecurityContextHolder.clear();
        final WMPBaseEventConsumer consumer = new WMPBaseEventConsumer();
        consumer.setObjectMapper(objectMapper);
        final AtomicReference<WMPContext> contextSeenByConsumer = new AtomicReference<>();
        final AtomicReference<SamplePayload> payloadSeenByConsumer = new AtomicReference<>();

        consumer.withEventPayload(cloudEvent, SamplePayload.class, payload -> {
            contextSeenByConsumer.set(SecurityContextHolder.getContext());
            payloadSeenByConsumer.set(payload);
        });

        assertEquals("hello", payloadSeenByConsumer.get().value());
        assertEquals("BANK_A", contextSeenByConsumer.get().getTenantId());
        assertEquals("BRANCH_01", contextSeenByConsumer.get().getBranchCode());
        assertEquals("user-1", contextSeenByConsumer.get().getUserId());
        // the consumer clears the context once the event is handled
        assertNull(SecurityContextHolder.getContext());
    }
}
