package raff.stein.platformcore.messaging.publisher;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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

    public WMPBaseEventPublisher(
            Producer<String, CloudEvent> kafkaCloudEventProducer,
            ObjectMapper objectMapper,
            @Value("${kafka.producer.source}") String producerSource) {
        this.kafkaCloudEventProducer = kafkaCloudEventProducer;
        this.objectMapper = objectMapper;
        this.producerSource = URI.create(StringUtils.hasText(producerSource) ? producerSource : "");
    }

    @Override
    public void publishCloudEvent(@NonNull String topic, @NonNull EventData eventData) {

        CloudEvent cloudEvent = createCloudEvent(eventData);
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
                cloudEventBuilder.withExtension("userid", wmpContext.getUserId());
            }
            if (StringUtils.hasText(wmpContext.getEmail())) {
                cloudEventBuilder.withExtension("email", wmpContext.getEmail());
            }
            if (StringUtils.hasText(wmpContext.getBankCode())) {
                cloudEventBuilder.withExtension("bankcode", wmpContext.getBankCode());
            }
            if (StringUtils.hasText(wmpContext.getCorrelationId())) {
                cloudEventBuilder.withExtension("correlationid", wmpContext.getCorrelationId());
            }
        }
        return cloudEventBuilder.build();
    }
}
