package raff.stein.profiler.service.assignment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openapitools.model.RoleAssignmentGrantedEvent;
import org.openapitools.model.RoleAssignmentRevokedEvent;
import org.springframework.context.ApplicationEventPublisher;
import raff.stein.platformcore.exception.types.conflict.ResourceConflictException;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;
import raff.stein.platformcore.security.context.RecentAuthenticationGuard;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.jwt.TokenRevocationService;
import raff.stein.profiler.client.bank.BankBranchValidator;
import raff.stein.profiler.model.assignment.AssignRoleCommand;
import raff.stein.profiler.model.assignment.AssignmentResult;
import raff.stein.profiler.model.assignment.RoleAssignment;
import raff.stein.profiler.model.entity.KnownUserEntity;
import raff.stein.profiler.model.entity.RoleAssignmentEntity;
import raff.stein.profiler.model.entity.RoleAssignmentRequestEntity;
import raff.stein.profiler.model.entity.enumeration.RoleAssignmentRequestStatus;
import raff.stein.profiler.repository.KnownUserRepository;
import raff.stein.profiler.repository.RoleAssignmentRepository;
import raff.stein.profiler.repository.RoleAssignmentRequestRepository;
import raff.stein.profiler.service.PermissionService;
import raff.stein.profiler.service.assignment.policy.AdvisorAssignmentPolicy;
import raff.stein.profiler.service.assignment.policy.AssignmentPolicyDispatcher;
import raff.stein.profiler.service.assignment.policy.BankAdminAssignmentPolicy;
import raff.stein.profiler.service.assignment.policy.PlatformAdminAssignmentPolicy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleAssignmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");
    private static final UUID TARGET = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final String TARGET_EMAIL = "customer@bank.test";

    private RoleAssignmentRepository assignmentRepository;
    private RoleAssignmentRequestRepository requestRepository;
    private KnownUserRepository knownUserRepository;
    private BankBranchValidator bankBranchValidator;
    private RecentAuthenticationGuard recentAuthenticationGuard;
    private TokenRevocationService tokenRevocationService;
    private PermissionService permissionService;
    private ApplicationEventPublisher eventPublisher;
    private RoleAssignmentService service;

    @BeforeEach
    void setUp() {
        assignmentRepository = mock(RoleAssignmentRepository.class);
        requestRepository = mock(RoleAssignmentRequestRepository.class);
        knownUserRepository = mock(KnownUserRepository.class);
        bankBranchValidator = mock(BankBranchValidator.class);
        recentAuthenticationGuard = mock(RecentAuthenticationGuard.class);
        tokenRevocationService = mock(TokenRevocationService.class);
        permissionService = mock(PermissionService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new RoleAssignmentService(
                assignmentRepository, requestRepository, knownUserRepository,
                new AssignmentPolicyDispatcher(List.of(
                        new AdvisorAssignmentPolicy(), new BankAdminAssignmentPolicy(), new PlatformAdminAssignmentPolicy())),
                bankBranchValidator, recentAuthenticationGuard, tokenRevocationService, permissionService,
                new RoleAssignmentProperties(), eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
        when(knownUserRepository.findById(TARGET))
                .thenReturn(Optional.of(KnownUserEntity.builder().userId(TARGET).email(TARGET_EMAIL).build()));
        when(assignmentRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), 7L));
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    void advisorShouldLinkCustomerToOwnBranchImmediately() {
        givenCaller("advisor-1", "ADVISOR", "BANK_A", "BR_01");

        final AssignmentResult result = service.assign(command("CUSTOMER", "BR_01", null));

        assertEquals(AssignmentResult.Outcome.ASSIGNED, result.getOutcome());
        verify(bankBranchValidator).requireActiveBranch("BANK_A", "BR_01");
        verify(recentAuthenticationGuard, never()).requireAuthenticatedWithin(any());
        verify(permissionService).syncUserPermissions(eq(TARGET_EMAIL), eq("BANK_A"), eq("BR_01"), any());
        final ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals("CUSTOMER", ((RoleAssignmentGrantedEvent) event.getValue()).getRole());
    }

    @Test
    void shouldRefuseAssigningARoleToYourself() {
        givenCaller(TARGET.toString(), "BANK_ADMIN", "BANK_A", "BR_01");

        assertThrows(AccessDeniedException.class, () -> service.assign(command("CUSTOMER", "BR_01", null)));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void shouldRefuseAssigningPlatformAdmin() {
        givenCaller("pa-1", "PLATFORM_ADMIN", "PLATFORM", "PLATFORM");

        assertThrows(AccessDeniedException.class, () -> service.assign(command("PLATFORM_ADMIN", "BR_01", "BANK_A")));
    }

    @Test
    void bankAdminShouldRequestApprovalForAdvisorAfterRecentLoginCheck() {
        givenCaller("ba-1", "BANK_ADMIN", "BANK_A", "BR_01");

        final AssignmentResult result = service.assign(command("ADVISOR", "BR_01", null));

        assertEquals(AssignmentResult.Outcome.PENDING_APPROVAL, result.getOutcome());
        assertEquals(RoleAssignmentRequestStatus.PENDING, result.getRequest().getStatus());
        verify(recentAuthenticationGuard).requireAuthenticatedWithin(Duration.ofMinutes(10));
        verify(assignmentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldRefuseDuplicateAssignment() {
        givenCaller("advisor-1", "ADVISOR", "BANK_A", "BR_01");
        when(assignmentRepository.existsByUserIdAndBankCodeAndBranchCodeAndRole(TARGET, "BANK_A", "BR_01", "CUSTOMER"))
                .thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> service.assign(command("CUSTOMER", "BR_01", null)));
    }

    @Test
    void requesterShouldNotApproveItsOwnRequest() {
        givenPendingRequest("ba-1", NOW.minusSeconds(60));
        givenCaller("ba-1", "BANK_ADMIN", "BANK_A", "BR_01");

        assertThrows(AccessDeniedException.class, () -> service.approve(1L));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void secondBankAdminShouldApproveAndCreateTheAssignment() {
        final RoleAssignmentRequestEntity request = givenPendingRequest("ba-1", NOW.minusSeconds(60));
        givenCaller("ba-2", "BANK_ADMIN", "BANK_A", "BR_02");

        final RoleAssignment assignment = service.approve(1L);

        assertEquals("ADVISOR", assignment.getRole());
        assertEquals("ba-2@bank.test", assignment.getApprovedBy());
        assertEquals("ba-1@bank.test", assignment.getGrantedBy());
        assertEquals(RoleAssignmentRequestStatus.APPROVED, request.getStatus());
    }

    @Test
    void adminOfAnotherBankShouldNotApprove() {
        givenPendingRequest("ba-1", NOW.minusSeconds(60));
        givenCaller("ba-9", "BANK_ADMIN", "BANK_B", "BR_09");

        assertThrows(AccessDeniedException.class, () -> service.approve(1L));
    }

    @Test
    void oldPendingRequestShouldExpireInsteadOfBeingApproved() {
        final RoleAssignmentRequestEntity request = givenPendingRequest("ba-1", NOW.minus(Duration.ofDays(8)));
        givenCaller("ba-2", "BANK_ADMIN", "BANK_A", "BR_02");

        assertThrows(ResourceConflictException.class, () -> service.approve(1L));
        assertEquals(RoleAssignmentRequestStatus.EXPIRED, request.getStatus());
    }

    @Test
    void revokingShouldEndEveryTokenOfTheUser() {
        givenCaller("advisor-1", "ADVISOR", "BANK_A", "BR_01");
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(RoleAssignmentEntity.builder()
                .id(5L).userId(TARGET).userEmail(TARGET_EMAIL).bankCode("BANK_A").branchCode("BR_01").role("CUSTOMER")
                .grantedBy("advisor-1@bank.test").build()));

        service.revoke(5L);

        verify(tokenRevocationService).revokeAllIssuedUpTo(TARGET.toString(), NOW.getEpochSecond());
        verify(permissionService).syncUserPermissions(eq(TARGET_EMAIL), eq("BANK_A"), eq("BR_01"), eq(List.of()));
        verify(eventPublisher).publishEvent(any(RoleAssignmentRevokedEvent.class));
    }

    @Test
    void advisorShouldNotRevokeAnAdvisor() {
        givenCaller("advisor-1", "ADVISOR", "BANK_A", "BR_01");
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(RoleAssignmentEntity.builder()
                .id(5L).userId(TARGET).userEmail(TARGET_EMAIL).bankCode("BANK_A").branchCode("BR_01").role("ADVISOR")
                .grantedBy("ba-1@bank.test").build()));

        assertThrows(AccessDeniedException.class, () -> service.revoke(5L));
        verify(tokenRevocationService, never()).revokeAllIssuedUpTo(anyString(), org.mockito.ArgumentMatchers.anyLong());
    }

    private RoleAssignmentRequestEntity givenPendingRequest(String requesterId, Instant requestedAt) {
        final RoleAssignmentRequestEntity request = RoleAssignmentRequestEntity.builder()
                .id(1L).userId(TARGET).userEmail(TARGET_EMAIL).bankCode("BANK_A").branchCode("BR_01").role("ADVISOR")
                .status(RoleAssignmentRequestStatus.PENDING)
                .requestedBy(requesterId + "@bank.test").requestedByUserId(requesterId).requestedAt(requestedAt)
                .build();
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        return request;
    }

    private static void givenCaller(String userId, String role, String bankCode, String branchCode) {
        SecurityContextHolder.setContext(WMPContext.builder()
                .userId(userId).email(userId + "@bank.test").roles(Set.of(role))
                .tenantId(bankCode).branchCode(branchCode)
                .build());
    }

    private static AssignRoleCommand command(String role, String branchCode, String bankCode) {
        return AssignRoleCommand.builder().userId(TARGET).role(role).branchCode(branchCode).bankCode(bankCode).build();
    }

    // simulates the generated id the database would assign
    private static RoleAssignmentEntity withId(RoleAssignmentEntity entity, Long id) {
        return RoleAssignmentEntity.builder()
                .id(id).userId(entity.getUserId()).userEmail(entity.getUserEmail())
                .bankCode(entity.getBankCode()).branchCode(entity.getBranchCode()).role(entity.getRole())
                .grantedBy(entity.getGrantedBy()).approvedBy(entity.getApprovedBy()).requestId(entity.getRequestId())
                .build();
    }
}
