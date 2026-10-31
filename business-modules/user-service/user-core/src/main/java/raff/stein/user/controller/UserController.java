package raff.stein.user.controller;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.UserApi;
import org.openapitools.model.UserDTO;
import org.openapitools.model.UserSummaryDTO;
import org.openapitools.model.UserSummaryPageDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import raff.stein.user.controller.mapper.UserToUserDTOMapper;
import raff.stein.user.model.user.User;
import raff.stein.user.service.UserDirectoryService;
import raff.stein.user.service.UserService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;
    private final UserDirectoryService userDirectoryService;

    private static final UserToUserDTOMapper userToUserDTOMapper = UserToUserDTOMapper.MAPPER;

    @Override
    @PreAuthorize("hasRole('ADVISOR')")
    public ResponseEntity<UserDTO> createUser(UserDTO userDTO) {
        User userInput = userToUserDTOMapper.toUser(userDTO);
        User createdUser = userService.createUser(userInput);
        UserDTO responseUserDTO = userToUserDTOMapper.toUserDto(createdUser);
        return ResponseEntity
                .created(URI.create("/users/" + responseUserDTO.getId()))
                .body(responseUserDTO);
    }

    @Override
    @PreAuthorize("hasRole('ADVISOR')")
    public ResponseEntity<Void> disableUser(UUID id) {
        boolean disabled = userService.disableUser(id);
        if (disabled) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    @PreAuthorize("hasRole('ADVISOR')")
    public ResponseEntity<Void> enableUser(UUID id) {
        boolean enabled = userService.enableUser(id);
        if (enabled) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    // every signed-in user, admins included, reads its own profile
    @PreAuthorize("hasAnyRole('CUSTOMER','ADVISOR','BANK_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<UserDTO> getCurrentUser() {
        User currentUser = userService.getCurrentUser();
        UserDTO userDTO = userToUserDTOMapper.toUserDto(currentUser);
        return ResponseEntity.ok(userDTO);
    }

    @Override
    // scoped in UserDirectoryService: a customer reads only itself, staff only users in their scope
    @PreAuthorize("hasAnyRole('CUSTOMER','ADVISOR','BANK_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<UserDTO> getUserById(UUID id) {
        User user = userDirectoryService.getVisibleUser(id);
        UserDTO userDTO = userToUserDTOMapper.toUserDto(user);
        return ResponseEntity.ok(userDTO);
    }

    @Override
    @PreAuthorize("hasRole('ADVISOR')")
    public ResponseEntity<UserDTO> updateUserById(UUID id, UserDTO userDTO) {
        User userInput = userToUserDTOMapper.toUser(userDTO);
        User updatedUser = userService.updateUserById(id, userInput);
        UserDTO responseUserDTO = userToUserDTOMapper.toUserDto(updatedUser);
        return ResponseEntity.ok(responseUserDTO);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADVISOR','BANK_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<UserSummaryDTO> lookupUserByEmail(String email) {
        return ResponseEntity.ok(userToUserDTOMapper.toUserSummaryDto(userDirectoryService.lookupByEmail(email)));
    }

    @Override
    @PreAuthorize("hasAnyRole('BANK_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<UserSummaryPageDTO> searchUsers(String query, Boolean linked, Integer page, Integer size) {
        return ResponseEntity.ok(userToUserDTOMapper.toUserSummaryPageDto(userDirectoryService.search(
                query, linked, page == null ? 0 : page, size == null ? 20 : size)));
    }
}
