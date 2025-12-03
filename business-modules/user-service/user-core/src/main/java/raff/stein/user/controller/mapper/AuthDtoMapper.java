package raff.stein.user.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.AuthResponseDTO;
import org.openapitools.model.LoginCredentialsDTO;
import org.openapitools.model.RegisterRequestDTO;
import raff.stein.user.model.auth.AuthResponse;
import raff.stein.user.model.auth.LoginRequest;
import raff.stein.user.model.auth.RegisterRequest;

@Mapper(config = UserControllerCommonMapperConfig.class)
public interface AuthDtoMapper {

    AuthDtoMapper MAPPER = Mappers.getMapper(AuthDtoMapper.class);

    // Login
    @Mapping(target = "email", source = "email")
    @Mapping(target = "password", source = "password")
    LoginRequest toLoginRequest(LoginCredentialsDTO dto);

    // Register
    RegisterRequest toRegisterRequest(RegisterRequestDTO dto);

    // Auth response
    AuthResponseDTO toAuthResponseDTO(AuthResponse domain);
}

