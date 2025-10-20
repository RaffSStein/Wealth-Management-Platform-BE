package raff.stein.profiler.event.consumer;

import io.cloudevents.CloudEvent;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.UserCreatedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.messaging.consumer.WMPBaseEventConsumer;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.topics.user-service.user-created.enabled", havingValue = "true")
public class UserCreatedEventConsumer extends WMPBaseEventConsumer {

    @KafkaListener(
            topics = "${kafka.topics.user-service.user-created.name}",
            containerFactory = "kafkaListenerFactory",
            groupId = "${kafka.topics.user-service.user-created.groupId}")
    public void consume(CloudEvent cloudEvent) {
        Optional<UserCreatedEvent> eventData = getEventPayload(cloudEvent, UserCreatedEvent.class);
        if(eventData.isEmpty()) {
            log.warn("Received empty payload from user-created topic for eventId: [{}]", cloudEvent.getId());
        }
        eventData.ifPresent(this::processUserCreatedEvent);
    }

    private void processUserCreatedEvent(@NotNull UserCreatedEvent userCreatedEvent) {
        //TODO: Implement the logic to handle the UserCreatedEvent and save its permissions based on some info
    }
}
