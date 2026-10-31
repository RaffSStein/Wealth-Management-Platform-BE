package raff.stein.user.event.consumer;

import io.cloudevents.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.RoleAssignmentRevokedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.messaging.consumer.WMPBaseEventConsumer;
import raff.stein.user.service.RoleAssignmentReadModelService;

/**
 * Sessions were already ended by profiler (shared token store); this only keeps login from issuing the role again.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.topics.profiler-service.role-assignment-revoked.enabled", havingValue = "true")
public class RoleAssignmentRevokedEventConsumer extends WMPBaseEventConsumer {

    private final RoleAssignmentReadModelService readModelService;

    @KafkaListener(
            topics = "${kafka.topics.profiler-service.role-assignment-revoked.name}",
            containerFactory = "kafkaListenerFactory",
            groupId = "${kafka.topics.profiler-service.role-assignment-revoked.groupId}")
    public void consume(CloudEvent cloudEvent) {
        withEventPayload(cloudEvent, RoleAssignmentRevokedEvent.class, event -> {
            if (event.getAssignmentId() == null) {
                log.warn("Skipping RoleAssignmentRevokedEvent without assignment id, eventId: [{}]", cloudEvent.getId());
                return;
            }
            readModelService.applyRevoked(event.getAssignmentId());
        });
    }
}
