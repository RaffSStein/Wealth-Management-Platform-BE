package raff.stein.profiler.event.producer;

import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.RoleAssignmentGrantedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import raff.stein.platformcore.messaging.publisher.WMPBaseEventPublisher;
import raff.stein.platformcore.messaging.publisher.model.EventData;

/**
 * The service raises the event inside its transaction; it goes to Kafka only after the commit, so a rolled-back
 * assignment is never announced. No outbox yet: a crash between commit and send loses the event.
 */
@Slf4j
@Component
public class RoleAssignmentGrantedEventPublisher {

    private final WMPBaseEventPublisher wmpBaseEventPublisher;
    private final String topic;

    public RoleAssignmentGrantedEventPublisher(
            WMPBaseEventPublisher wmpBaseEventPublisher,
            @Value("${kafka.topics.profiler-service.role-assignment-granted.name}") String topic) {
        this.wmpBaseEventPublisher = wmpBaseEventPublisher;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publish(RoleAssignmentGrantedEvent event) {
        wmpBaseEventPublisher.publishCloudEvent(topic, new EventData(event));
        log.info("Published RoleAssignmentGrantedEvent for assignmentId: [{}], role: [{}]",
                event.getAssignmentId(), event.getRole());
    }
}
