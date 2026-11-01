package raff.stein.customer.event.producer;

import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.CreateUserEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import raff.stein.platformcore.messaging.publisher.WMPBaseEventPublisher;
import raff.stein.platformcore.messaging.publisher.model.EventData;

/**
 * The service raises the event inside its transaction; it goes to Kafka only after the commit, so a rolled-back record
 * never creates a login. No outbox yet: a crash between commit and send loses the event.
 */
@Slf4j
@Component
public class CreateUserEventPublisher {

    private final WMPBaseEventPublisher wmpBaseEventPublisher;
    private final String topic;

    public CreateUserEventPublisher(
            WMPBaseEventPublisher wmpBaseEventPublisher,
            @Value("${kafka.topics.user-service.create-user.name}") String topic) {
        this.wmpBaseEventPublisher = wmpBaseEventPublisher;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publish(CreateUserEvent event) {
        wmpBaseEventPublisher.publishCloudEvent(topic, new EventData(event));
        log.info("Published CreateUserEvent for branch: [{}/{}]", event.getBankCode(), event.getBranchCode());
    }
}
