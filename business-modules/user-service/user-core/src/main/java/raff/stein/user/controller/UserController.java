package raff.stein.user.controller;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.UserApi;
import org.openapitools.model.UserDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import raff.stein.user.controller.mapper.UserToUserDTOMapper;
import raff.stein.user.model.User;
import raff.stein.user.service.UserService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    private static final UserToUserDTOMapper userToUserDTOMapper = UserToUserDTOMapper.MAPPER;

    //TODO: Implement the methods of UserApi interface

    @Override
    public ResponseEntity<UserDTO> createUser(UserDTO userDTO) {
        User userInput = userToUserDTOMapper.toUser(userDTO);
        User createdUser = userService.createUser(userInput);
        UserDTO responseUserDTO = userToUserDTOMapper.toUserDto(createdUser);
        return ResponseEntity.created(URI.create("/users/" + responseUserDTO.getId())).body(responseUserDTO);
    }

    @Override
    public ResponseEntity<Void> disableUser(UUID id) {
        boolean disabled = userService.disableUser(id);
        if (disabled) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<Void> enableUser(UUID id) {
        boolean enabled = userService.enableUser(id);
        if (enabled) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<UserDTO> getCurrentUser() {
        User currentUser = userService.getCurrentUser();
        UserDTO userDTO = userToUserDTOMapper.toUserDto(currentUser);
        return ResponseEntity.ok(userDTO);
    }

    @Override
    public ResponseEntity<UserDTO> getUserById(UUID id) {
        User user = userService.getUserById(id);
        UserDTO userDTO = userToUserDTOMapper.toUserDto(user);
        return ResponseEntity.ok(userDTO);
    }

    @Override
    public ResponseEntity<UserDTO> updateUserById(UUID id, UserDTO userDTO) {
        User userInput = userToUserDTOMapper.toUser(userDTO);
        User updatedUser = userService.updateUserById(id, userInput);
        UserDTO responseUserDTO = userToUserDTOMapper.toUserDto(updatedUser);
        return ResponseEntity.ok(responseUserDTO);
    }


}
