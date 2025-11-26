package raff.stein.user.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.user.event.producer.UserCreatedEventPublisher;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.model.entity.mapper.UserToUserEntityMapper;
import raff.stein.user.model.user.User;
import raff.stein.user.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserCreatedEventPublisher userCreatedEventPublisher;

    private static final UserToUserEntityMapper userToUserEntityMapper = UserToUserEntityMapper.MAPPER;

    @Transactional
    public User createUser(User user) {
        final User savedUser = createUserEntity(user);
        // TODO: generate onboarding token for user creation request that comes from an advisor
        publishUser(savedUser, null);
        return savedUser;
    }

    @Transactional
    public User createUserEntity(User user) {
        log.debug("Creating user entity: [{}]", user);
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("User with email already exists: " + user.getEmail());
        }
        UserEntity entity = userToUserEntityMapper.toUserEntity(user);
        UserEntity savedUserEntity = userRepository.save(entity);
        final User savedUser = userToUserEntityMapper.toUser(savedUserEntity);
        log.info("Created user with ID: [{}], email: [{}]", savedUser.getId(), savedUser.getEmail());
        return savedUser;
    }

    public void publishUser(User user, @Nullable String onboardingToken) {
        userCreatedEventPublisher.publishUserCreatedEvent(user, onboardingToken);
    }

    public boolean disableUser(UUID id) {
        // TODO: Implement user disable logic
        return false;
    }

    public boolean enableUser(UUID id) {
        // TODO: Implement user enable logic
        return false;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        // retrieve user data by context
        final WMPContext context = SecurityContextHolder.getContextOrThrow();
        final String userEmail = context.getEmail();
        log.debug("Retrieving current user with email: [{}]", userEmail);
        final UserEntity userEntity = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + userEmail));
        log.debug("Found user entity: [{}]", userEntity);
        return userToUserEntityMapper.toUser(userEntity);
    }

    public User getUserById(UUID id) {
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
        return userToUserEntityMapper.toUser(userEntity);
    }

    @Transactional
    public User updateUserById(UUID id, User user) {
        //TODO: to complete
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
                // Update fields of userEntity with values from user
        userEntity.setFirstName(user.getFirstName());
        userEntity.setEmail(user.getEmail());
        // Add other fields as needed
        UserEntity savedUserEntity = userRepository.save(userEntity);
        return userToUserEntityMapper.toUser(savedUserEntity);
    }
}
