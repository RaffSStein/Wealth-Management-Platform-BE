package raff.stein.user.event.consumer;

import io.cloudevents.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.RoleAssignmentGrantedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.messaging.consumer.WMPBaseEventConsumer;
import raff.stein.user.service.RoleAssignmentReadModelService;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.topics.profiler-service.role-assignment-granted.enabled", havingValue = "true")
public class RoleAssignmentGrantedEventConsumer extends WMPBaseEventConsumer {

    private final RoleAssignmentReadModelService readModelService;

    @KafkaListener(
            topics = "${kafka.topics.profiler-service.role-assignment-granted.name}",
            containerFactory = "kafkaListenerFactory",
            groupId = "${kafka.topics.profiler-service.role-assignment-granted.groupId}")
    public void consume(CloudEvent cloudEvent) {
        withEventPayload(cloudEvent, RoleAssignmentGrantedEvent.class, event -> {
            if (event.getAssignmentId() == null || event.getUserId() == null
                    || event.getBankCode() == null || event.getBranchCode() == null || event.getRole() == null) {
                log.warn("Skipping RoleAssignmentGrantedEvent with missing fields, eventId: [{}]", cloudEvent.getId());
                return;
            }
            readModelService.applyGranted(event.getAssignmentId(), event.getUserId(),
                    event.getBankCode(), event.getBranchCode(), event.getRole());
        });
    }
}
