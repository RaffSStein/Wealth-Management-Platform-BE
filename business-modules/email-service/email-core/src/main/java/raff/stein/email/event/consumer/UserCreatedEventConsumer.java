package raff.stein.email.event.consumer;

import io.cloudevents.CloudEvent;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.User;
import org.openapitools.model.UserCreatedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import raff.stein.email.service.EmailService;
import raff.stein.platformcore.messaging.consumer.WMPBaseEventConsumer;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.topics.user-service.user-created.enabled", havingValue = "true")
public class UserCreatedEventConsumer extends WMPBaseEventConsumer {

    private final EmailService emailService;

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
        final User user = userCreatedEvent.getUser();
        log.info("Processing UserCreatedEvent for userId: [{}], email: [{}]",
                userCreatedEvent.getUserId(),
                user.getEmail());
        final String userEmail = user.getEmail();
        final String userName = user.getFirstName();
        final String onboardingToken = userCreatedEvent.getOnboardingToken();
        try {
            emailService.sendOnboardingEmail(userEmail, userName, onboardingToken);
            log.info("Onboarding email sent to user: [{}]", userEmail);
        } catch (Exception e) {
            log.error("Failed to send onboarding email to user: [{}]. Error: {}", userEmail, e.getMessage());
        }
    }
}
