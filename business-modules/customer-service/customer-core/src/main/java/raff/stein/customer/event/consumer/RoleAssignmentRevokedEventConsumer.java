package raff.stein.customer.event.consumer;

import io.cloudevents.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.RoleAssignmentRevokedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import raff.stein.customer.service.link.CustomerUserLinkService;
import raff.stein.platformcore.messaging.consumer.WMPBaseEventConsumer;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.role.WmpRole;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.topics.profiler-service.role-assignment-revoked.enabled", havingValue = "true")
public class RoleAssignmentRevokedEventConsumer extends WMPBaseEventConsumer {

    private final CustomerUserLinkService customerUserLinkService;

    @KafkaListener(
            topics = "${kafka.topics.profiler-service.role-assignment-revoked.name}",
            containerFactory = "kafkaListenerFactory",
            groupId = "${kafka.topics.profiler-service.role-assignment-revoked.groupId}")
    public void consume(CloudEvent cloudEvent) {
        withEventPayload(cloudEvent, RoleAssignmentRevokedEvent.class, event -> {
            if (event.getUserId() == null || event.getBankCode() == null
                    || event.getBranchCode() == null || event.getRole() == null) {
                log.warn("Skipping RoleAssignmentRevokedEvent with missing fields, eventId: [{}]", cloudEvent.getId());
                return;
            }
            if (WmpRole.parse(event.getRole()).filter(role -> role == WmpRole.CUSTOMER).isEmpty()) {
                return;
            }
            // the lookup runs in the event's tenant: a different bank would search the wrong customers
            final WMPContext context = SecurityContextHolder.getContext();
            if (context == null || !event.getBankCode().equals(context.getTenantId())) {
                log.warn("Skipping RoleAssignmentRevokedEvent for another tenant, eventId: [{}]", cloudEvent.getId());
                return;
            }
            customerUserLinkService.unlinkOnRoleRevoked(event.getUserId(), event.getBranchCode());
        });
    }
}
