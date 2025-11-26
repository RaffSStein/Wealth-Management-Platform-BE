package raff.stein.user.event.consumer;

import io.cloudevents.CloudEvent;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.CreateUserEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.messaging.consumer.WMPBaseEventConsumer;
import raff.stein.user.event.consumer.mapper.CreateUserEventToUserMapper;
import raff.stein.user.event.producer.UserCreatedEventPublisher;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.model.entity.mapper.UserToUserEntityMapper;
import raff.stein.user.model.user.User;
import raff.stein.user.repository.UserRepository;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.topics.user-service.create-user.enabled", havingValue = "true")
public class CreateUserEventConsumer extends WMPBaseEventConsumer {

    private static final CreateUserEventToUserMapper createUserEventToUserMapper = CreateUserEventToUserMapper.MAPPER;
    private static final UserToUserEntityMapper userToUserEntityMapper = UserToUserEntityMapper.MAPPER;
    private final UserRepository userRepository;
    private final UserCreatedEventPublisher userCreatedEventPublisher;

    @KafkaListener(
            topics = "${kafka.topics.user-service.create-user.name}",
            containerFactory = "kafkaListenerFactory",
            groupId = "${kafka.topics.user-service.create-user.groupId}")
    public void consume(CloudEvent cloudEvent) {
        Optional<CreateUserEvent> eventData = getEventPayload(cloudEvent, CreateUserEvent.class);
        if(eventData.isEmpty()) {
            log.warn("Received empty payload from create-user topic for eventId: [{}]", cloudEvent.getId());
        }
        eventData.ifPresent(e -> processCreateUserEvent(e, cloudEvent.getId()));
    }


    private void processCreateUserEvent(@NotNull CreateUserEvent createUserEvent, String eventId) {
        log.info("Processing CreateUserEvent with eventId: [{}]", eventId);
        User user = createUserEventToUserMapper.toUser(createUserEvent);
        // check if this user already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            log.warn("User with email [{}] already exists. Skipping CreateUserEvent with eventId: [{}]",
                    user.getEmail(),
                    eventId);
            return;
        }
        //TODO: this should use UserService.createUser to avoid code duplication and to handle the token
        log.info("Creating new user with email: [{}]", user.getEmail());
        // Map User to UserEntity
        UserEntity userEntity = userToUserEntityMapper.toUserEntity(user);
        // get the saved user entity for the id
        UserEntity savedUser = userRepository.save(userEntity);
        log.info("Successfully processed CreateUserEvent with eventId: [{}]", eventId);
        // Publish UserCreatedEvent
        log.info("Publishing UserCreatedEvent for user with email: [{}]", user.getEmail());
        // Map UserEntity back to User
        User userToBePublished = userToUserEntityMapper.toUser(savedUser);
        // Publish the user just created
        userCreatedEventPublisher.publishUserCreatedEvent(userToBePublished, null);
    }
}
