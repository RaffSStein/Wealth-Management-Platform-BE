package raff.stein.user.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.jwt.TokenRevocationService;
import raff.stein.user.model.BranchRole;
import raff.stein.user.model.auth.*;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.model.user.User;
import raff.stein.user.repository.UserRepository;
import raff.stein.user.security.JwtTokenIssuer;
import raff.stein.user.security.WmpUserDetails;
import raff.stein.user.service.PasswordService;
import raff.stein.user.service.PasswordSetupTokenService;
import raff.stein.user.service.UserService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserService userService;
    private final PasswordService passwordService;
    private final UserRepository userRepository;
    private final JwtTokenIssuer jwtTokenIssuer;
    private final AuthenticationManager authenticationManager;
    private final PasswordSetupTokenService passwordSetupTokenService;
    private final TokenRevocationService tokenRevocationService;

    @Transactional
    public void register(RegisterRequest request) {
        // TODO: use a mapper, add branch roles association
        final User user = userService.createUserEntity(
                User.builder()
                        .email(request.getEmail())
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .phoneNumber(request.getPhoneNumber())
                        .branchRoles(mapBranchRoleRequests(request.getBranchRoles()))
                        .build());
        // Issue a dedicated password setup token
        String setupToken = jwtTokenIssuer.issuePasswordSetupToken(
                user.getId(),
                user.getEmail()
        );
        userService.publishUser(user, setupToken);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword())
        );

        WmpUserDetails principal = (WmpUserDetails) auth.getPrincipal();
        List<String> tokenRoles = auth.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .distinct()
                .toList();
        if (tokenRoles.isEmpty()) {
            log.warn("Role fallback applied during login auth: assigning CUSTOMER for email [{}] (no authorities found)",
                    request.getEmail());
            tokenRoles = List.of(WmpRole.CUSTOMER.name());
        }
        String token = jwtTokenIssuer.issueToken(
                principal.getUserId(),
                principal.getUsername(),
                tokenRoles,
                buildExtraClaims(principal.getUserId()));
        log.info("User [{}] logged in successfully", request.getEmail());
        return new AuthResponse(token, "Bearer", jwtTokenIssuer.getExpirationSeconds());
    }

    @Transactional
    public void setupPassword(String password) {
        final WMPContext context = SecurityContextHolder.getContext();
        final String userEmail = context.getEmail();
        final String rawToken = context.getRawToken();
        final UserEntity userEntity = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + userEmail));
        final List<String> violations = passwordService.validatePolicy(password, userEmail);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Password does not meet policy requirements: " + String.join(", ", violations));
        }
        // Validate and consume the password setup token to avoid future reuse
        passwordSetupTokenService.validateAndConsume(rawToken);
        final String hashedPassword = passwordService.encode(password);
        userEntity.setPasswordHash(hashedPassword);
        userRepository.save(userEntity);
    }

    public void logout() {
        WMPContext context = SecurityContextHolder.getContext();
        if (context == null) {
            return; // nothing to revoke
        }
        String jti = context.getJti();
        Long expEpoch = context.getTokenExpEpochSeconds();
        if (jti != null && expEpoch != null) {
            tokenRevocationService.revoke(jti, expEpoch);
        }
    }

    // --- Helpers --------------------------------------------------------------------

    private List<BranchRole> mapBranchRoleRequests(List<BranchRoleRequest> requests) {
        if (requests == null) return List.of();
        return requests.stream()
                .map(br -> BranchRole.builder()
                        .bankCode(br.getBankCode())
                        .bankId(br.getBankId())
                        .role(br.getRole())
                        .build())
                .toList();
    }

    private Map<String, Object> buildExtraClaims(String userId) {
        return Map.of("userId", userId);
    }
}
