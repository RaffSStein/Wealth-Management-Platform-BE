package raff.stein.user.event.producer;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.UserCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.messaging.publisher.WMPBaseEventPublisher;
import raff.stein.platformcore.messaging.publisher.model.EventData;
import raff.stein.user.event.producer.mapper.UserToUserCreatedEventMapper;
import raff.stein.user.model.User;

@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.topics.user-service.user-created.enabled", havingValue = "true")
public class UserCreatedEventPublisher {

    private final WMPBaseEventPublisher wmpBaseEventPublisher;
    private final String userCreatedTopic;
    private static final UserToUserCreatedEventMapper userToUserCreatedEventMapper = UserToUserCreatedEventMapper.MAPPER;

    public UserCreatedEventPublisher(
            WMPBaseEventPublisher wmpBaseEventPublisher,
            @Value("${kafka.topics.user-service.user-created.name}") String userCreatedTopic) {
        this.wmpBaseEventPublisher = wmpBaseEventPublisher;
        this.userCreatedTopic = userCreatedTopic;
    }

    public void publishUserCreatedEvent(@NotNull User user) {
        UserCreatedEvent userCreatedEvent = userToUserCreatedEventMapper.toUserCreatedEvent(user);
        EventData eventData = new EventData(userCreatedEvent);
        wmpBaseEventPublisher.publishCloudEvent(userCreatedTopic, eventData);
        log.info("Published UserCreatedEvent for user with ID: [{}]", user.getId());
    }


}
