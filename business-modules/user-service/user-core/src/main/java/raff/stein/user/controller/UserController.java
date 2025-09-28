package raff.stein.user.controller;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.UserApi;
import org.openapitools.model.UserDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import raff.stein.user.service.UserService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    //TODO: Implement the methods of UserApi interface

    @Override
    public ResponseEntity<UserDTO> createUser(UserDTO userDTO) {
        return null;
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
        UserDTO currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            return ResponseEntity.ok(currentUser);
        } else {
            return ResponseEntity.status(401).build();
        }
    }

    @Override
    public ResponseEntity<UserDTO> getUserById(UUID id) {
        UserDTO user = userService.getUserById(id);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<UserDTO> updateUserById(UUID id, UserDTO userDTO) {
        return null;
    }


}
