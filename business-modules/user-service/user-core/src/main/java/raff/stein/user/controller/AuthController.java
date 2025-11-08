package raff.stein.user.controller;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.AuthApi;
import org.openapitools.model.AuthResponseDTO;
import org.openapitools.model.LoginCredentialsDTO;
import org.openapitools.model.RegisterRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import raff.stein.user.controller.mapper.AuthDtoMapper;
import raff.stein.user.model.auth.AuthResponse;
import raff.stein.user.model.auth.LoginRequest;
import raff.stein.user.model.auth.RegisterRequest;
import raff.stein.user.service.auth.AuthenticationService;

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
    public ResponseEntity<AuthResponseDTO> registerUser(RegisterRequestDTO registerRequestDTO) {
        RegisterRequest request = authDtoMapper.toRegisterRequest(registerRequestDTO);
        AuthResponse response = authenticationService.register(request);
        return ResponseEntity.ok(authDtoMapper.toAuthResponseDTO(response));
    }
}
