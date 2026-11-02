package raff.stein.profiler.event.producer;

import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.RoleAssignmentRevokedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.messaging.publisher.EventPublishingListener;
import raff.stein.platformcore.messaging.publisher.WMPBaseEventPublisher;
import raff.stein.platformcore.messaging.publisher.model.EventData;

/**
 * Raised inside the transaction, like {@link RoleAssignmentGrantedEventPublisher}.
 */
@Slf4j
@Component
public class RoleAssignmentRevokedEventPublisher {

    private final WMPBaseEventPublisher wmpBaseEventPublisher;
    private final String topic;

    public RoleAssignmentRevokedEventPublisher(
            WMPBaseEventPublisher wmpBaseEventPublisher,
            @Value("${kafka.topics.profiler-service.role-assignment-revoked.name}") String topic) {
        this.wmpBaseEventPublisher = wmpBaseEventPublisher;
        this.topic = topic;
    }

    @EventPublishingListener
    public void publish(RoleAssignmentRevokedEvent event) {
        wmpBaseEventPublisher.publishCloudEvent(topic, new EventData(event));
        log.info("Handed over RoleAssignmentRevokedEvent for assignmentId: [{}], role: [{}]",
                event.getAssignmentId(), event.getRole());
    }
}
