package raff.stein.user.event.consumer;

import io.cloudevents.CloudEvent;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.CreateUserEvent;
import org.openapitools.model.CustomerUserResolvedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import raff.stein.platformcore.messaging.consumer.WMPBaseEventConsumer;
import raff.stein.user.event.consumer.mapper.CreateUserEventToUserMapper;
import raff.stein.user.event.producer.CustomerUserResolvedEventPublisher;
import raff.stein.user.event.producer.UserCreatedEventPublisher;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.model.entity.mapper.UserToUserEntityMapper;
import raff.stein.user.model.user.User;
import raff.stein.user.repository.UserRepository;
import raff.stein.user.security.JwtTokenIssuer;

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
    private final CustomerUserResolvedEventPublisher customerUserResolvedEventPublisher;
    private final JwtTokenIssuer jwtTokenIssuer;
    private final TransactionTemplate transactionTemplate;

    @KafkaListener(
            topics = "${kafka.topics.user-service.create-user.name}",
            containerFactory = "kafkaListenerFactory",
            groupId = "${kafka.topics.user-service.create-user.groupId}")
    public void consume(CloudEvent cloudEvent) {
        Optional<CreateUserEvent> eventData = getEventPayload(cloudEvent, CreateUserEvent.class);
        if(eventData.isEmpty()) {
            log.warn("Received empty payload from create-user topic for eventId: [{}]", cloudEvent.getId());
        }
        // one transaction: the user and the events about it are stored together, so a crash in between can't leave
        // a user nobody was told about. A redelivery then finds the user, or finds nothing and starts over
        eventData.ifPresent(e -> transactionTemplate.executeWithoutResult(status -> processCreateUserEvent(e, cloudEvent.getId())));
    }


    private void processCreateUserEvent(@NotNull CreateUserEvent createUserEvent, String eventId) {
        log.info("Processing CreateUserEvent with eventId: [{}]", eventId);
        final User user = createUserEventToUserMapper.toUser(createUserEvent);
        final boolean forClient = createUserEvent.getBankCode() != null && createUserEvent.getBranchCode() != null;
        final Optional<UserEntity> existingUserOptional = userRepository.findByEmailIgnoreCase(user.getEmail());
        final UserEntity existingUserEntity;
        if (existingUserOptional.isPresent()) {
            // no new login and no setup email: replays and clients who already have an account land here
            log.info("User already exists. Nothing to create for CreateUserEvent with eventId: [{}]", eventId);
            existingUserEntity = existingUserOptional.get();
        } else {
            existingUserEntity = userRepository.save(userToUserEntityMapper.toUserEntity(user));
            final String userId = existingUserEntity.getId().toString();
            final String setupToken = forClient
                    ? jwtTokenIssuer.issueClientSetupToken(userId, existingUserEntity.getEmail())
                    : jwtTokenIssuer.issuePasswordSetupToken(userId, existingUserEntity.getEmail());
            userCreatedEventPublisher.publishUserCreatedEvent(userToUserEntityMapper.toUser(existingUserEntity), setupToken);
            log.info("Created user [{}] for CreateUserEvent with eventId: [{}]", userId, eventId);
        }
        if (forClient) {
            // only id and email: there's no session here, and the role assignments of a found user are lazy
            customerUserResolvedEventPublisher.publish(new CustomerUserResolvedEvent()
                    .userId(existingUserEntity.getId())
                    .email(existingUserEntity.getEmail())
                    .bankCode(createUserEvent.getBankCode())
                    .branchCode(createUserEvent.getBranchCode())
                    .requestedBy(createUserEvent.getRequestedBy()));
        }
    }
}
