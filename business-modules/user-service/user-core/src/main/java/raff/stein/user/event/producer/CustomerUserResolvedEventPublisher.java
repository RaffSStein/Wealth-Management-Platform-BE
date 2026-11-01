package raff.stein.user.event.producer;

import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.CustomerUserResolvedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.messaging.publisher.WMPBaseEventPublisher;
import raff.stein.platformcore.messaging.publisher.model.EventData;

@Slf4j
@Component
public class CustomerUserResolvedEventPublisher {

    private final WMPBaseEventPublisher wmpBaseEventPublisher;
    private final String topic;

    public CustomerUserResolvedEventPublisher(
            WMPBaseEventPublisher wmpBaseEventPublisher,
            @Value("${kafka.topics.user-service.customer-user-resolved.name}") String topic) {
        this.wmpBaseEventPublisher = wmpBaseEventPublisher;
        this.topic = topic;
    }

    public void publish(CustomerUserResolvedEvent event) {
        wmpBaseEventPublisher.publishCloudEvent(topic, new EventData(event));
        log.info("Published CustomerUserResolvedEvent for userId: [{}]", event.getUserId());
    }
}
