package raff.stein.customer.event.consumer;

import io.cloudevents.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.CustomerOnboardedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.messaging.consumer.WMPBaseEventConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.topics.customer-service.customer-onboarded.consuming-enabled", havingValue = "true")
public class CustomerOnboardedEventConsumer extends WMPBaseEventConsumer {


    @KafkaListener(
            topics = "${kafka.topics.customer-service.customer-onboarded.name}",
            containerFactory = "kafkaListenerFactory",
            groupId = "${kafka.topics.customer-service.customer-onboarded.groupId}")
    public void consume(CloudEvent cloudEvent) {
        withEventPayload(
                cloudEvent,
                CustomerOnboardedEvent.class,
                payload -> processFileValidatedEvent(payload, cloudEvent.getId()));
    }

    private void processFileValidatedEvent(CustomerOnboardedEvent customerOnboardedEvent, String eventId) {
        // TODO: trigger AML verification

    }

}
