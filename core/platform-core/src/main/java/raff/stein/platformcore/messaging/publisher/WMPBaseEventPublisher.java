package raff.stein.platformcore.messaging.publisher;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import raff.stein.platformcore.messaging.CloudEventExtensions;
import raff.stein.platformcore.messaging.outbox.OutboxEventWriter;
import raff.stein.platformcore.messaging.publisher.model.EventData;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Component
public class WMPBaseEventPublisher implements EventPublisher {

    private final Producer<String, CloudEvent> kafkaCloudEventProducer;
    private final ObjectMapper objectMapper;
    private final URI producerSource;
    // only present when platform.outbox.enabled=true; without it events go straight to Kafka
    private OutboxEventWriter outboxEventWriter;

    public WMPBaseEventPublisher(
            Producer<String, CloudEvent> kafkaCloudEventProducer,
            ObjectMapper objectMapper,
            @Value("${kafka.producer.source}") String producerSource) {
        this.kafkaCloudEventProducer = kafkaCloudEventProducer;
        this.objectMapper = objectMapper;
        this.producerSource = URI.create(StringUtils.hasText(producerSource) ? producerSource : "");
    }

    @Autowired(required = false)
    public void setOutboxEventWriter(OutboxEventWriter outboxEventWriter) {
        this.outboxEventWriter = outboxEventWriter;
    }

    /**
     * Call it from inside the business transaction (see {@link EventPublishingListener}). The caller doesn't need to
     * know whether the outbox is on:
     * <ul>
     *   <li>outbox on: the event is stored in the caller's transaction and the relay sends it later, at least once;</li>
     *   <li>outbox off, inside a transaction: the event is held until the commit and dropped on a rollback, so
     *   a rolled-back change is never announced (a crash before the send still loses it, which is the outbox's job);</li>
     *   <li>outbox off, no transaction: it is sent at once. Failures are only logged.</li>
     * </ul>
     */
    @Override
    public void publishCloudEvent(@NonNull String topic, @NonNull EventData eventData) {

        CloudEvent cloudEvent = createCloudEvent(eventData);

        if (outboxEventWriter != null) {
            outboxEventWriter.store(topic, cloudEvent);
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendNow(topic, cloudEvent);
                }
            });
            return;
        }

        sendNow(topic, cloudEvent);
    }

    private void sendNow(@NonNull String topic, CloudEvent cloudEvent) {
        log.info("Publishing CloudEvent to topic: [{}], cloudEventId: [{}], cloudEventType: [{}]",
                topic,
                cloudEvent.getId(),
                cloudEvent.getType());

        sendCloudEvent(topic, cloudEvent);
    }

    private void sendCloudEvent(@NonNull String topic, CloudEvent cloudEvent) {
        kafkaCloudEventProducer.send(
                new ProducerRecord<>(topic, cloudEvent), (recordMetadata, e) -> {
                    if (e != null) {
                        log.error("Failed to publish CloudEvent to topic: [{}], error: {}", topic, e.getMessage());
                    } else {
                        log.info("CloudEvent published to topic: [{}], partition: [{}], offset: [{}]",
                                topic, recordMetadata.partition(), recordMetadata.offset());
                    }
                }
        );
    }

    private CloudEvent createCloudEvent(@NonNull EventData eventData) {

        Object data = eventData.data();
        CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                .withDataContentType("application/json")
                .withType(data.getClass().getName())
                .withSource(producerSource)
                .withData(PojoCloudEventData.wrap(data, objectMapper::writeValueAsBytes))
                .withTime(OffsetDateTime.now(ZoneOffset.UTC))
                .withId(UUID.randomUUID().toString())
                .withSubject("");

        // forward user information from the security context
        final WMPContext wmpContext = SecurityContextHolder.getContext();

        if (wmpContext != null) {
            // add extensions only if values are present to avoid builder errors
            if (StringUtils.hasText(wmpContext.getUserId())) {
                cloudEventBuilder.withExtension(CloudEventExtensions.USER_ID, wmpContext.getUserId());
            }
            if (StringUtils.hasText(wmpContext.getEmail())) {
                cloudEventBuilder.withExtension(CloudEventExtensions.EMAIL, wmpContext.getEmail());
            }
            // consumers restore the tenant from here, so their writes stay in the producer's tenant
            if (StringUtils.hasText(wmpContext.getTenantId())) {
                cloudEventBuilder.withExtension(CloudEventExtensions.TENANT_ID, wmpContext.getTenantId());
            }
            if (StringUtils.hasText(wmpContext.getBranchCode())) {
                cloudEventBuilder.withExtension(CloudEventExtensions.BRANCH_CODE, wmpContext.getBranchCode());
            }
            if (StringUtils.hasText(wmpContext.getCorrelationId())) {
                cloudEventBuilder.withExtension(CloudEventExtensions.CORRELATION_ID, wmpContext.getCorrelationId());
            }
        }
        return cloudEventBuilder.build();
    }
}
