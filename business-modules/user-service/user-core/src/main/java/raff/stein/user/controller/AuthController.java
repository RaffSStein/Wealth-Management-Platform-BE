package raff.stein.user.controller;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.AuthApi;
import org.openapitools.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import raff.stein.user.controller.mapper.AuthDtoMapper;
import raff.stein.user.model.auth.AuthResponse;
import raff.stein.user.model.auth.LoginRequest;
import raff.stein.user.model.auth.RegisterRequest;
import raff.stein.user.service.auth.AuthenticationService;

import java.net.URI;

/**
 * Authentication endpoints (login & register) specific to user-service.
 */
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthenticationService authenticationService;

    private static final AuthDtoMapper authDtoMapper = AuthDtoMapper.MAPPER;

    @Override
    public ResponseEntity<AuthResponseDTO> loginUser(LoginCredentialsDTO loginCredentialsDTO) {
        LoginRequest request = authDtoMapper.toLoginRequest(loginCredentialsDTO);
        AuthResponse response = authenticationService.login(request);
        return ResponseEntity.ok(authDtoMapper.toAuthResponseDTO(response));
    }

    @Override
    public ResponseEntity<Void> registerUser(RegisterRequestDTO registerRequestDTO) {
        RegisterRequest request = authDtoMapper.toRegisterRequest(registerRequestDTO);
        authenticationService.register(request);
        return ResponseEntity.created(URI.create("/user")).build();
    }

    @Override
    public ResponseEntity<Void> requestPasswordReset(PasswordResetRequestDTO passwordResetRequestDTO) {
        // TODO: implement reset request: trigger email with JWT link
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> setupPassword(SetupPasswordDTO setupPasswordDTO) {
        // TODO: implement setup: validate JWT (setupPasswordDTO.token), persist password, enable login
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
