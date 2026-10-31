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
import raff.stein.profiler.model.entity.KnownUserEntity;
import raff.stein.profiler.service.assignment.KnownUserService;
import raff.stein.profiler.service.assignment.RoleAssignmentProperties;
import raff.stein.profiler.service.assignment.RoleAssignmentService;

import java.util.Optional;

/**
 * Records every new user so it can receive role assignments. Roles are no longer taken from this event: they come
 * only from role assignments. Configured bootstrap emails get PLATFORM_ADMIN (first platform run).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.topics.user-service.user-created.enabled", havingValue = "true")
public class UserCreatedEventConsumer extends WMPBaseEventConsumer {

    private final KnownUserService knownUserService;
    private final RoleAssignmentService roleAssignmentService;
    private final RoleAssignmentProperties roleAssignmentProperties;

    @KafkaListener(
            topics = "${kafka.topics.user-service.user-created.name}",
            containerFactory = "kafkaListenerFactory",
            groupId = "${kafka.topics.user-service.user-created.groupId}")
    public void consume(CloudEvent cloudEvent) {
        Optional<UserCreatedEvent> eventData = getEventPayload(cloudEvent, UserCreatedEvent.class);
        if (eventData.isEmpty()) {
            log.warn("Received empty payload from user-created topic for eventId: [{}]", cloudEvent.getId());
        }
        eventData.ifPresent(this::processUserCreatedEvent);
    }

    private void processUserCreatedEvent(@NotNull UserCreatedEvent userCreatedEvent) {
        if (userCreatedEvent.getUserId() == null || userCreatedEvent.getUser() == null
                || userCreatedEvent.getUser().getEmail() == null) {
            log.warn("Skipping UserCreatedEvent without user id or email");
            return;
        }
        final String email = userCreatedEvent.getUser().getEmail();
        log.info("Processing UserCreatedEvent for userId: [{}]", userCreatedEvent.getUserId());
        final KnownUserEntity user = knownUserService.recordUser(userCreatedEvent.getUserId(), email);
        if (roleAssignmentProperties.getBootstrapPlatformAdmins().stream().anyMatch(email::equalsIgnoreCase)) {
            roleAssignmentService.bootstrapPlatformAdmin(user);
        }
    }
}
