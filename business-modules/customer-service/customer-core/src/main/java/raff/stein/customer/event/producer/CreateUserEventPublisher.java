package raff.stein.customer.event.producer;

import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.CreateUserEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.messaging.publisher.EventPublishingListener;
import raff.stein.platformcore.messaging.publisher.WMPBaseEventPublisher;
import raff.stein.platformcore.messaging.publisher.model.EventData;

/**
 * The service raises the event inside its transaction and the outbox stores it with the customer, so a rolled-back
 * record never creates a login and a crash after the commit doesn't lose the event.
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

    @EventPublishingListener
    public void publish(CreateUserEvent event) {
        wmpBaseEventPublisher.publishCloudEvent(topic, new EventData(event));
        log.info("Handed over CreateUserEvent for branch: [{}/{}]", event.getBankCode(), event.getBranchCode());
    }
}
