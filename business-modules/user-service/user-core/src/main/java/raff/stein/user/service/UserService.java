package raff.stein.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.user.model.User;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.model.entity.mapper.UserToUserEntityMapper;
import raff.stein.user.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private static final UserToUserEntityMapper userToUserEntityMapper = UserToUserEntityMapper.MAPPER;


    @Transactional
    public User createUser(User user) {
        log.debug("Creating user: [{}]", user);
        UserEntity savedUserEntity = userRepository.save(userToUserEntityMapper.toUserEntity(user));
        return userToUserEntityMapper.toUser(savedUserEntity);
    }

    public boolean disableUser(UUID id) {
        // TODO: Implement user disable logic
        return false;
    }

    public boolean enableUser(UUID id) {
        // TODO: Implement user enable logic
        return false;
    }

    public User getCurrentUser() {
        // TODO: Implement logic to retrieve current logged-in user
        return null;
    }

    public User getUserById(UUID id) {
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
        return userToUserEntityMapper.toUser(userEntity);
    }

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
