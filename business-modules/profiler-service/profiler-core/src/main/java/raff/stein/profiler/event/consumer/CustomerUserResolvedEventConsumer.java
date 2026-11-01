package raff.stein.profiler.event.consumer;

import io.cloudevents.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.CustomerUserResolvedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.messaging.consumer.WMPBaseEventConsumer;
import raff.stein.profiler.model.entity.KnownUserEntity;
import raff.stein.profiler.service.assignment.KnownUserService;
import raff.stein.profiler.service.assignment.RoleAssignmentService;

/**
 * An advisor created a client and its login exists now. The user is recorded here too, because this event may arrive
 * before user-created.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.topics.user-service.customer-user-resolved.enabled", havingValue = "true")
public class CustomerUserResolvedEventConsumer extends WMPBaseEventConsumer {

    private final KnownUserService knownUserService;
    private final RoleAssignmentService roleAssignmentService;

    @KafkaListener(
            topics = "${kafka.topics.user-service.customer-user-resolved.name}",
            containerFactory = "kafkaListenerFactory",
            groupId = "${kafka.topics.user-service.customer-user-resolved.groupId}")
    public void consume(CloudEvent cloudEvent) {
        withEventPayload(cloudEvent, CustomerUserResolvedEvent.class, event -> {
            if (event.getUserId() == null || event.getEmail() == null
                    || event.getBankCode() == null || event.getBranchCode() == null) {
                log.warn("Skipping CustomerUserResolvedEvent with missing fields, eventId: [{}]", cloudEvent.getId());
                return;
            }
            final KnownUserEntity client = knownUserService.recordUser(event.getUserId(), event.getEmail());
            roleAssignmentService.grantCustomerToClient(
                    client, event.getBankCode(), event.getBranchCode(), event.getRequestedBy());
        });
    }
}
