package raff.stein.user.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.user.event.producer.UserCreatedEventPublisher;
import raff.stein.user.model.BranchRole;
import raff.stein.user.model.auth.AuthResponse;
import raff.stein.user.model.auth.LoginRequest;
import raff.stein.user.model.auth.PlatformRole;
import raff.stein.user.model.auth.RegisterRequest;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.model.user.User;
import raff.stein.user.repository.UserRepository;
import raff.stein.user.security.JwtTokenIssuer;
import raff.stein.user.security.PlatformUserDetails;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenIssuer jwtTokenIssuer;
    private final UserCreatedEventPublisher userCreatedEventPublisher;
    private final AuthenticationManager authenticationManager; // from platform-core

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        UserEntity entity = UserEntity.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .phoneNumber(request.getPhoneNumber())
                .lastName(request.getLastName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .build();
        userRepository.save(entity);

        List<BranchRole> branchRoles = mapBranchRoleRequests(request.getBranchRoles());

        // Build domain user for event
        User domainUser = User.builder()
                .id(String.valueOf(entity.getId()))
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .phoneNumber(entity.getPhoneNumber())
                .branchRoles(branchRoles)
                .build();
        userCreatedEventPublisher.publishUserCreatedEvent(domainUser);

        // Derive token roles directly from provided branch roles (avoid extra authenticate/query)
        List<String> tokenRoles = rolesFromBranchRoles(branchRoles);
        if (tokenRoles.isEmpty()) {
            log.warn("Role fallback applied during registration: assigning CUSTOMER for email [{}] (no branch roles provided)", request.getEmail());
            tokenRoles = List.of(PlatformRole.CUSTOMER.name());
        }
        String userId = String.valueOf(entity.getId());
        String bankCodeClaim = branchRoles.isEmpty() ? null : branchRoles.get(0).getBankCode();
        //TODO: remove issuing for registered users if email verification is needed
        String token = jwtTokenIssuer.issueToken(
                userId,
                entity.getEmail(),
                tokenRoles,
                buildExtraClaims(userId, bankCodeClaim));
        return new AuthResponse(token, "Bearer", jwtTokenIssuer.getExpirationSeconds());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        PlatformUserDetails principal = (PlatformUserDetails) auth.getPrincipal();
        List<String> tokenRoles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .distinct()
                .toList();
        if (tokenRoles.isEmpty()) {
            log.warn("Role fallback applied during login auth: assigning CUSTOMER for email [{}] (no authorities found)",
                    request.getEmail());
            tokenRoles = List.of(PlatformRole.CUSTOMER.name());
        }
        String token = jwtTokenIssuer.issueToken(
                principal.getUserId(),
                principal.getUsername(),
                tokenRoles,
                buildExtraClaims(principal.getUserId(), request.getBankCode()));
        return new AuthResponse(token, "Bearer", jwtTokenIssuer.getExpirationSeconds());
    }

    // --- Helpers --------------------------------------------------------------------

    private List<String> rolesFromBranchRoles(List<BranchRole> branchRoles) {
        if (branchRoles == null || branchRoles.isEmpty()) return List.of();
        Set<PlatformRole> roles = branchRoles.stream()
                .map(BranchRole::getRole)
                .map(PlatformRole::fromString)
                .collect(java.util.stream.Collectors.toSet());
        return roles.stream().map(Enum::name).sorted().toList();
    }

    private List<BranchRole> mapBranchRoleRequests(List<raff.stein.user.model.auth.BranchRoleRequest> requests) {
        if (requests == null) return List.of();
        return requests.stream()
                .map(br -> BranchRole.builder()
                        .bankCode(br.getBankCode())
                        .bankId(br.getBankId())
                        .role(br.getRole())
                        .build())
                .toList();
    }

    private Map<String, Object> buildExtraClaims(String userId, String bankCode) {
        if (bankCode == null || bankCode.isBlank()) {
            return Map.of("userId", userId);
        }
        return Map.of("userId", userId, "bankCode", bankCode);
    }
}
