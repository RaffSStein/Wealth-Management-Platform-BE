package raff.stein.user.service.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.jwt.TokenRevocationService;
import raff.stein.platformcore.security.jwt.WmpJwtClaims;
import raff.stein.user.model.auth.AuthResponse;
import raff.stein.user.model.auth.LoginRequest;
import raff.stein.user.model.auth.RegisterRequest;
import raff.stein.user.model.user.User;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private UserService userService;
    private PasswordService passwordService;
    private PasswordSetupTokenService passwordSetupTokenService;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        jwtTokenIssuer = mock(JwtTokenIssuer.class);
        tokenRevocationService = mock(TokenRevocationService.class);
        userService = mock(UserService.class);
        passwordService = mock(PasswordService.class);
        passwordSetupTokenService = mock(PasswordSetupTokenService.class);
        authenticationService = new AuthenticationService(
                userService,
                passwordService,
                userRepository,
                jwtTokenIssuer,
                mock(AuthenticationManager.class),
                passwordSetupTokenService,
                tokenRevocationService);
        when(jwtTokenIssuer.issueToken(anyString(), anyString(), any(), any())).thenReturn("new-token");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    void shouldIssueTokenForFirstAssignmentWhenNoBranchRequested() {
        givenUser(assignment(1L, "BANK_A", "BR_01", "ADVISOR"), assignment(2L, "BANK_B", "BR_09", "CUSTOMER"));

        authenticationService.login(loginRequest(null, null));

        final IssuedToken token = captureIssuedToken();
        assertEquals("BANK_A", token.claims().get(WmpJwtClaims.TENANT_ID));
        assertEquals("BR_01", token.claims().get(WmpJwtClaims.BRANCH_CODE));
        assertEquals(List.of("ADVISOR"), token.roles());
    }

    @Test
    void shouldIssueTokenWithOnlyTheRequestedBranchRoles() {
        givenUser(assignment(1L, "BANK_A", "BR_01", "ADVISOR"), assignment(2L, "BANK_B", "BR_09", "CUSTOMER"));

        authenticationService.login(loginRequest(null, "BR_09"));

        final IssuedToken token = captureIssuedToken();
        assertEquals("BANK_B", token.claims().get(WmpJwtClaims.TENANT_ID));
        assertEquals("BR_09", token.claims().get(WmpJwtClaims.BRANCH_CODE));
        assertEquals(List.of("CUSTOMER"), token.roles());
    }

    @Test
    void shouldUseBankCodeToDisambiguateSameBranchCodeInTwoBanks() {
        givenUser(assignment(1L, "BANK_A", "BR_01", "ADVISOR"), assignment(2L, "BANK_B", "BR_01", "CUSTOMER"));

        authenticationService.login(loginRequest("BANK_B", "BR_01"));

        final IssuedToken token = captureIssuedToken();
        assertEquals("BANK_B", token.claims().get(WmpJwtClaims.TENANT_ID));
        assertEquals(List.of("CUSTOMER"), token.roles());
    }

    @Test
    void shouldDenyLoginToBranchWithoutAssignment() {
        givenUser(assignment(1L, "BANK_A", "BR_01", "ADVISOR"));

        assertThrows(AccessDeniedException.class, () -> authenticationService.login(loginRequest(null, "BR_99")));
    }

    @Test
    void shouldIssueCustomerTokenWithoutTenantWhenUserHasNoAssignment() {
        givenUser();

        authenticationService.login(loginRequest(null, null));

        final IssuedToken token = captureIssuedToken();
        assertFalse(token.claims().containsKey(WmpJwtClaims.TENANT_ID));
        assertEquals(List.of("CUSTOMER"), token.roles());
    }

    @Test
    void shouldSwitchBranchAndRevokeCurrentToken() {
        givenUser(assignment(1L, "BANK_A", "BR_01", "ADVISOR"), assignment(2L, "BANK_B", "BR_09", "ADVISOR"));
        SecurityContextHolder.setContext(currentSession());

        final AuthResponse response = authenticationService.switchBranch(null, "BR_09");

        assertEquals("new-token", response.getToken());
        assertEquals("BANK_B", captureIssuedToken().claims().get(WmpJwtClaims.TENANT_ID));
        verify(tokenRevocationService).revoke("current-jti", 1_900_000_000L);
    }

    @Test
    void shouldKeepCurrentTokenWhenSwitchIsDenied() {
        givenUser(assignment(1L, "BANK_A", "BR_01", "ADVISOR"));
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

    @Test
    void shouldRegisterUserWithoutAnyRoleAssignment() {
        when(userService.createUserEntity(any())).thenAnswer(invocation -> {
            final User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID().toString());
            return user;
        });
        final RegisterRequest request = new RegisterRequest();
        request.setEmail("new@user.test");
        request.setFirstName("New");
        request.setLastName("User");
        request.setPhoneNumber("000");

        authenticationService.register(request);

        final ArgumentCaptor<User> created = ArgumentCaptor.forClass(User.class);
        verify(userService).createUserEntity(created.capture());
        assertTrue(created.getValue().getBranchRoles().isEmpty());
    }

    @Test
    void shouldRejectWeakPasswordWithoutConsumingTheSetupLink() {
        SecurityContextHolder.setContext(WMPContext.builder().email(EMAIL).rawToken("setup-token").build());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(UserEntity.builder().email(EMAIL).build()));
        when(passwordService.validatePolicy("weak", EMAIL)).thenReturn(List.of("Password must contain at least one digit"));

        assertThrows(RequestValidationException.class, () -> authenticationService.setupPassword("weak"));

        verify(passwordSetupTokenService, never()).validateAndConsume(anyString());
    }

    private void givenUser(BranchUserEntity... assignments) {
        final UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .bankBranchUsers(new HashSet<>(Set.of(assignments)))
                .build();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
    }

    private static BranchUserEntity assignment(Long id, String bankCode, String branchCode, String role) {
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
