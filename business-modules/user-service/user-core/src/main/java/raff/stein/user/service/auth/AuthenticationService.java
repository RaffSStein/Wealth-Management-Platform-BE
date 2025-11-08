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

import java.util.List;
import java.util.Map;

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
        // Persist new user with encoded password
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

        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        List<String> tokenRoles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .distinct()
                .toList();
        if (tokenRoles.isEmpty()) {
            log.warn("Role fallback applied during registration auth: assigning CUSTOMER for email [{}] (no authorities found)", request.getEmail());
            tokenRoles = List.of(PlatformRole.CUSTOMER.name());
        }

        String bankCodeClaim = branchRoles.isEmpty() ? null : branchRoles.get(0).getBankCode();
        String token = jwtTokenIssuer.issueToken(String.valueOf(entity.getId()), entity.getEmail(), tokenRoles, buildExtraClaims(String.valueOf(entity.getId()), bankCodeClaim));
        return new AuthResponse(token, "Bearer", jwtTokenIssuer.getExpirationSeconds());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        UserEntity entity = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!entity.isEnabled()) {
            throw new IllegalStateException("Account disabled");
        }
        List<String> tokenRoles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .distinct()
                .toList();
        if (tokenRoles.isEmpty()) {
            log.warn("Role fallback applied during login auth: assigning CUSTOMER for email [{}] (no authorities found)", request.getEmail());
            tokenRoles = List.of(PlatformRole.CUSTOMER.name());
        }
        String token = jwtTokenIssuer.issueToken(String.valueOf(entity.getId()), entity.getEmail(), tokenRoles, buildExtraClaims(String.valueOf(entity.getId()), request.getBankCode()));
        return new AuthResponse(token, "Bearer", jwtTokenIssuer.getExpirationSeconds());
    }

    // --- Helpers --------------------------------------------------------------------

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
