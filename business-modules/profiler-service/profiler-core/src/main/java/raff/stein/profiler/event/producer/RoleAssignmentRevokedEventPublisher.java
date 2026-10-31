package raff.stein.profiler.event.producer;

import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.RoleAssignmentRevokedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import raff.stein.platformcore.messaging.publisher.WMPBaseEventPublisher;
import raff.stein.platformcore.messaging.publisher.model.EventData;

/**
 * Sent after the commit, like {@link RoleAssignmentGrantedEventPublisher}.
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publish(RoleAssignmentRevokedEvent event) {
        wmpBaseEventPublisher.publishCloudEvent(topic, new EventData(event));
        log.info("Published RoleAssignmentRevokedEvent for assignmentId: [{}], role: [{}]",
                event.getAssignmentId(), event.getRole());
    }
}
