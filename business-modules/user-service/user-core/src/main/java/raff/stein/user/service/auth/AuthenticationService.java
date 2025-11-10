package raff.stein.user.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.user.model.BranchRole;
import raff.stein.user.model.auth.*;
import raff.stein.user.model.user.User;
import raff.stein.user.security.JwtTokenIssuer;
import raff.stein.user.security.WmpUserDetails;
import raff.stein.user.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserService userService;

    private final JwtTokenIssuer jwtTokenIssuer;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public void register(RegisterRequest request) {
        // TODO: use a mapper
        userService.createUser(
                User.builder()
                        .email(request.getEmail())
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .phoneNumber(request.getPhoneNumber())
                        .branchRoles(mapBranchRoleRequests(request.getBranchRoles()))
                        .build()
        );
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        WmpUserDetails principal = (WmpUserDetails) auth.getPrincipal();
        List<String> tokenRoles = auth.getAuthorities().stream()
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
                buildExtraClaims(principal.getUserId(), request.getBankCode()));
        return new AuthResponse(token, "Bearer", jwtTokenIssuer.getExpirationSeconds());
    }

    // --- Helpers --------------------------------------------------------------------

    private List<String> rolesFromBranchRoles(List<BranchRole> branchRoles) {
        if (branchRoles == null || branchRoles.isEmpty()) return List.of();
        Set<WmpRole> roles = branchRoles.stream()
                .map(BranchRole::getRole)
                .map(WmpRole::fromString)
                .collect(java.util.stream.Collectors.toSet());
        return roles.stream().map(Enum::name).sorted().toList();
    }

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

    private Map<String, Object> buildExtraClaims(String userId, String bankCode) {
        if (bankCode == null || bankCode.isBlank()) {
            return Map.of("userId", userId);
        }
        return Map.of("userId", userId, "bankCode", bankCode);
    }
}
