package raff.stein.user.service.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.jwt.TokenRevocationService;
import raff.stein.platformcore.security.jwt.WmpJwtClaims;
import raff.stein.user.model.auth.AuthResponse;
import raff.stein.user.model.auth.LoginRequest;
import raff.stein.user.model.entity.BranchUserEntity;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.repository.UserRepository;
import raff.stein.user.security.JwtTokenIssuer;
import raff.stein.user.service.PasswordService;
import raff.stein.user.service.PasswordSetupTokenService;
import raff.stein.user.service.UserService;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    private static final String EMAIL = "advisor@bank.test";

    private UserRepository userRepository;
    private JwtTokenIssuer jwtTokenIssuer;
    private TokenRevocationService tokenRevocationService;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        jwtTokenIssuer = mock(JwtTokenIssuer.class);
        tokenRevocationService = mock(TokenRevocationService.class);
        authenticationService = new AuthenticationService(
                mock(UserService.class),
                mock(PasswordService.class),
                userRepository,
                jwtTokenIssuer,
                mock(AuthenticationManager.class),
                mock(PasswordSetupTokenService.class),
                tokenRevocationService);
        when(jwtTokenIssuer.issueToken(anyString(), anyString(), any(), any())).thenReturn("new-token");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    void shouldIssueTokenForFirstMembershipWhenNoBranchRequested() {
        givenUser(membership(1L, "BANK_A", "BR_01", "ADVISOR"), membership(2L, "BANK_B", "BR_09", "CUSTOMER"));

        authenticationService.login(loginRequest(null, null));

        final IssuedToken token = captureIssuedToken();
        assertEquals("BANK_A", token.claims().get(WmpJwtClaims.TENANT_ID));
        assertEquals("BR_01", token.claims().get(WmpJwtClaims.BRANCH_CODE));
        assertEquals(List.of("ADVISOR"), token.roles());
    }

    @Test
    void shouldIssueTokenWithOnlyTheRequestedBranchRoles() {
        givenUser(membership(1L, "BANK_A", "BR_01", "ADVISOR"), membership(2L, "BANK_B", "BR_09", "CUSTOMER"));

        authenticationService.login(loginRequest(null, "BR_09"));

        final IssuedToken token = captureIssuedToken();
        assertEquals("BANK_B", token.claims().get(WmpJwtClaims.TENANT_ID));
        assertEquals("BR_09", token.claims().get(WmpJwtClaims.BRANCH_CODE));
        assertEquals(List.of("CUSTOMER"), token.roles());
    }

    @Test
    void shouldUseBankCodeToDisambiguateSameBranchCodeInTwoBanks() {
        givenUser(membership(1L, "BANK_A", "BR_01", "ADVISOR"), membership(2L, "BANK_B", "BR_01", "CUSTOMER"));

        authenticationService.login(loginRequest("BANK_B", "BR_01"));

        final IssuedToken token = captureIssuedToken();
        assertEquals("BANK_B", token.claims().get(WmpJwtClaims.TENANT_ID));
        assertEquals(List.of("CUSTOMER"), token.roles());
    }

    @Test
    void shouldDenyLoginToBranchWithoutMembership() {
        givenUser(membership(1L, "BANK_A", "BR_01", "ADVISOR"));

        assertThrows(AccessDeniedException.class, () -> authenticationService.login(loginRequest(null, "BR_99")));
    }

    @Test
    void shouldIssueCustomerTokenWithoutTenantWhenUserHasNoMembership() {
        givenUser();

        authenticationService.login(loginRequest(null, null));

        final IssuedToken token = captureIssuedToken();
        assertFalse(token.claims().containsKey(WmpJwtClaims.TENANT_ID));
        assertEquals(List.of("CUSTOMER"), token.roles());
    }

    @Test
    void shouldSwitchBranchAndRevokeCurrentToken() {
        givenUser(membership(1L, "BANK_A", "BR_01", "ADVISOR"), membership(2L, "BANK_B", "BR_09", "ADVISOR"));
        SecurityContextHolder.setContext(currentSession());

        final AuthResponse response = authenticationService.switchBranch(null, "BR_09");

        assertEquals("new-token", response.getToken());
        assertEquals("BANK_B", captureIssuedToken().claims().get(WmpJwtClaims.TENANT_ID));
        verify(tokenRevocationService).revoke("current-jti", 1_900_000_000L);
    }

    @Test
    void shouldKeepCurrentTokenWhenSwitchIsDenied() {
        givenUser(membership(1L, "BANK_A", "BR_01", "ADVISOR"));
        SecurityContextHolder.setContext(currentSession());

        assertThrows(AccessDeniedException.class, () -> authenticationService.switchBranch(null, "BR_99"));
        verify(tokenRevocationService, never()).revoke(anyString(), anyLong());
    }

    // --- helpers -----------------------------------------------------------------------

    private record IssuedToken(List<String> roles, Map<String, Object> claims) {
    }

    @SuppressWarnings("unchecked")
    private IssuedToken captureIssuedToken() {
        final ArgumentCaptor<List<String>> roles = ArgumentCaptor.forClass(List.class);
        final ArgumentCaptor<Map<String, Object>> claims = ArgumentCaptor.forClass(Map.class);
        verify(jwtTokenIssuer).issueToken(anyString(), eq(EMAIL), roles.capture(), claims.capture());
        return new IssuedToken(roles.getValue(), claims.getValue());
    }

    private void givenUser(BranchUserEntity... memberships) {
        final UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .bankBranchUsers(new HashSet<>(Set.of(memberships)))
                .build();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
    }

    private static BranchUserEntity membership(Long id, String bankCode, String branchCode, String role) {
        return BranchUserEntity.builder()
                .id(id)
                .bankCode(bankCode)
                .branchCode(branchCode)
                .role(role)
                .build();
    }

    private static LoginRequest loginRequest(String bankCode, String branchCode) {
        final LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword("secret");
        request.setBankCode(bankCode);
        request.setBranchCode(branchCode);
        return request;
    }

    private static WMPContext currentSession() {
        return WMPContext.builder()
                .email(EMAIL)
                .tenantId("BANK_A")
                .branchCode("BR_01")
                .jti("current-jti")
                .tokenExpEpochSeconds(1_900_000_000L)
                .build();
    }
}
