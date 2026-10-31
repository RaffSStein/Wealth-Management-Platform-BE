package raff.stein.user.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;
import raff.stein.platformcore.exception.types.unauthorized.WmpContextException;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.jwt.TokenRevocationService;
import raff.stein.platformcore.security.jwt.WmpJwtClaims;
import raff.stein.user.model.BranchRole;
import raff.stein.user.model.auth.*;
import raff.stein.user.model.entity.BranchUserEntity;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.model.user.User;
import raff.stein.user.repository.UserRepository;
import raff.stein.user.security.JwtTokenIssuer;
import raff.stein.user.service.PasswordService;
import raff.stein.user.service.PasswordSetupTokenService;
import raff.stein.user.service.UserService;

import java.util.Comparator;
import java.util.HashMap;
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

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // password check only: the token content comes from the active membership below
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword())
        );
        final UserEntity user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + request.getEmail()));
        final String token = issueTokenForMembership(user, request.getBankCode(), request.getBranchCode());
        log.info("User [{}] logged in successfully", request.getEmail());
        return new AuthResponse(token, "Bearer", jwtTokenIssuer.getExpirationSeconds());
    }

    /**
     * Issues a token for another membership of the current user and revokes the current token.
     */
    @Transactional(readOnly = true)
    public AuthResponse switchBranch(String bankCode, String branchCode) {
        final WMPContext context = SecurityContextHolder.getContext();
        if (context == null || context.getEmail() == null) {
            throw WmpContextException.forMissingField("email").get();
        }
        final UserEntity user = userRepository.findByEmailIgnoreCase(context.getEmail())
                .orElseThrow(GenericObjectNotFoundException.with(context.getEmail()));
        final String token = issueTokenForMembership(user, bankCode, branchCode);
        // only after the new token is issued, so a refused switch keeps the current session
        logout();
        log.info("User [{}] switched to branch [{}]", context.getEmail(), branchCode);
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
            // 400, and the token is not consumed yet, so the user can retry with the same link
            throw new RequestValidationException(
                    ErrorCode.REQUEST_VALIDATION_ERROR,
                    "Password does not meet policy requirements: " + String.join(", ", violations));
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
                        .branchCode(br.getBranchCode())
                        .role(br.getRole())
                        .build())
                .toList();
    }

    /**
     * Builds the token for one active membership: tenantId = its bank, branchCode = its branch,
     * roles = only the roles held in that membership.
     * Without a requested branch the first membership (lowest id) is used.
     */
    private String issueTokenForMembership(UserEntity user, String requestedBankCode, String requestedBranchCode) {
        final String userId = String.valueOf(user.getId());
        final Map<String, Object> claims = new HashMap<>();
        claims.put(WmpJwtClaims.USER_ID, userId);
        final List<BranchUserEntity> memberships = user.getBankBranchUsers().stream()
                .sorted(Comparator.comparing(BranchUserEntity::getId))
                .toList();
        if (memberships.isEmpty()) {
            if (requestedBranchCode != null) {
                throw membershipDenied(user.getEmail(), requestedBranchCode);
            }
            // no membership means no tenant: the user can't touch tenant-scoped data
            log.warn("Role fallback applied during token issuance: assigning CUSTOMER without tenant for user [{}] (no branch memberships)",
                    user.getEmail());
            return jwtTokenIssuer.issueToken(userId, user.getEmail(), List.of(WmpRole.CUSTOMER.name()), claims);
        }
        final BranchUserEntity active = requestedBranchCode == null
                ? memberships.getFirst()
                : memberships.stream()
                        .filter(m -> requestedBranchCode.equals(m.getBranchCode()))
                        .filter(m -> requestedBankCode == null || requestedBankCode.equals(m.getBankCode()))
                        .findFirst()
                        .orElseThrow(() -> membershipDenied(user.getEmail(), requestedBranchCode));
        final List<String> roles = memberships.stream()
                .filter(m -> m.getBankCode().equals(active.getBankCode()) && m.getBranchCode().equals(active.getBranchCode()))
                .map(m -> WmpRole.fromString(m.getRole()).name())
                .distinct()
                .toList();
        claims.put(WmpJwtClaims.TENANT_ID, active.getBankCode());
        claims.put(WmpJwtClaims.BRANCH_CODE, active.getBranchCode());
        return jwtTokenIssuer.issueToken(userId, user.getEmail(), roles, claims);
    }

    private static AccessDeniedException membershipDenied(String email, String branchCode) {
        return new AccessDeniedException(
                ErrorCode.ACCESS_DENIED,
                String.format(ErrorCode.ACCESS_DENIED.getMessageTemplate(), email, branchCode));
    }
}
