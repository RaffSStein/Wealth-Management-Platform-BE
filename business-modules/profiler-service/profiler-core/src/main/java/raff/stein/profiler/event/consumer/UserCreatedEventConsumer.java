package raff.stein.profiler.event.consumer;

import io.cloudevents.CloudEvent;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.openapitools.model.User;
import org.openapitools.model.UserBankBranch;
import org.openapitools.model.UserCreatedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.messaging.consumer.WMPBaseEventConsumer;
import raff.stein.profiler.service.PermissionService;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.topics.user-service.user-created.enabled", havingValue = "true")
public class UserCreatedEventConsumer extends WMPBaseEventConsumer {

    private final PermissionService permissionService;

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
        final List<UserBankBranch> bankBranches = user.getUserBankBranches();
        final String userEmail = user.getEmail();
        if (bankBranches == null || bankBranches.isEmpty()) {
            log.info("No bank branches associated with user: [{}]. Skipping permission setup.", userEmail);
            return;
        }

        permissionService.saveUserPermissions(
                userEmail,
                bankBranches.stream()
                        .map(branch -> new ImmutablePair<>(
                                branch.getBankCode(),
                                branch.getRole())
                        )
                        .toList());
    }
}
