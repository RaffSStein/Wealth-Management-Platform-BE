package raff.stein.profiler.service.assignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.RoleAssignmentGrantedEvent;
import org.openapitools.model.RoleAssignmentRevokedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;
import raff.stein.platformcore.exception.types.conflict.ResourceConflictException;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;
import raff.stein.platformcore.security.context.RecentAuthenticationGuard;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.jwt.TokenRevocationService;
import raff.stein.platformcore.security.role.WmpRole;
import raff.stein.profiler.client.bank.BankBranchValidator;
import raff.stein.profiler.model.assignment.AssignRoleCommand;
import raff.stein.profiler.model.assignment.AssignmentConstants;
import raff.stein.profiler.model.assignment.AssignmentResult;
import raff.stein.profiler.model.assignment.RoleAssignment;
import raff.stein.profiler.model.assignment.RoleAssignmentRequest;
import raff.stein.profiler.model.entity.KnownUserEntity;
import raff.stein.profiler.model.entity.RoleAssignmentEntity;
import raff.stein.profiler.model.entity.RoleAssignmentRequestEntity;
import raff.stein.profiler.model.entity.enumeration.RoleAssignmentRequestStatus;
import raff.stein.profiler.model.entity.mapper.RoleAssignmentEntityMapper;
import raff.stein.profiler.repository.KnownUserRepository;
import raff.stein.profiler.repository.RoleAssignmentRepository;
import raff.stein.profiler.repository.RoleAssignmentRequestRepository;
import raff.stein.profiler.service.assignment.policy.AssignmentDecision;
import raff.stein.profiler.service.assignment.policy.AssignmentDenied;
import raff.stein.profiler.service.assignment.policy.AssignmentPolicy;
import raff.stein.profiler.service.assignment.policy.AssignmentPolicyDispatcher;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Role assignments: who holds which role in which bank branch. Enforces the assignment matrix through the caller's
 * {@link AssignmentPolicy}, four-eyes approval for staff roles, the recent-login rule and the session cut on revoke.
 * See docs/domain/process/security/roles-and-permissions.md.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleAssignmentService {

    private final RoleAssignmentRepository assignmentRepository;
    private final RoleAssignmentRequestRepository requestRepository;
    private final KnownUserRepository knownUserRepository;
    private final AssignmentPolicyDispatcher policyDispatcher;
    private final BankBranchValidator bankBranchValidator;
    private final RecentAuthenticationGuard recentAuthenticationGuard;
    private final TokenRevocationService tokenRevocationService;
    private final RoleAssignmentProperties properties;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    private static final RoleAssignmentEntityMapper mapper = RoleAssignmentEntityMapper.MAPPER;

    @Transactional
    public AssignmentResult assign(AssignRoleCommand command) {
        final WMPContext caller = SecurityContextHolder.getContextOrThrow();
        final WmpRole role = parseRole(command.getRole());
        if (role == WmpRole.PLATFORM_ADMIN) {
            throw AssignmentDenied.because("PLATFORM_ADMIN is assigned only by the bootstrap");
        }
        final AssignmentPolicy policy = policyDispatcher.forCaller(caller);
        final AssignmentDecision decision = policy.decideAssignment(caller, role, command);
        if (isSamePerson(caller, command.getUserId())) {
            throw AssignmentDenied.because("nobody can assign a role to themselves");
        }
        final KnownUserEntity target = knownUserRepository.findById(command.getUserId())
                .orElseThrow(GenericObjectNotFoundException.with(String.valueOf(command.getUserId())));
        if (decision.requiresRecentLogin()) {
            recentAuthenticationGuard.requireAuthenticatedWithin(properties.getRecentLogin());
        }
        requireNoAssignmentOrPendingRequest(target.getUserId(), decision.bankCode(), decision.branchCode(), role);
        bankBranchValidator.requireActiveBranch(decision.bankCode(), decision.branchCode());

        if (decision.requiresApproval()) {
            final RoleAssignmentRequestEntity request = requestRepository.save(RoleAssignmentRequestEntity.builder()
                    .userId(target.getUserId())
                    .userEmail(target.getEmail())
                    .bankCode(decision.bankCode())
                    .branchCode(decision.branchCode())
                    .role(role.name())
                    .status(RoleAssignmentRequestStatus.PENDING)
                    .requestedBy(caller.getEmail())
                    .requestedByUserId(caller.getUserId())
                    .requestedAt(clock.instant())
                    .build());
            log.info("Role assignment request [{}] created: role [{}] in [{}/{}] for userId [{}], waiting for approval",
                    request.getId(), role, decision.bankCode(), decision.branchCode(), target.getUserId());
            return AssignmentResult.pending(mapper.toRoleAssignmentRequest(request));
        }
        final RoleAssignmentEntity assignment = saveAssignment(target, decision.bankCode(), decision.branchCode(), role,
                caller.getEmail(), null, null);
        return AssignmentResult.assigned(mapper.toRoleAssignment(assignment));
    }

    // the EXPIRED status must survive the 409 thrown for an expired request
    @Transactional(noRollbackFor = ResourceConflictException.class)
    public RoleAssignment approve(Long requestId) {
        final WMPContext caller = SecurityContextHolder.getContextOrThrow();
        final RoleAssignmentRequestEntity request = loadPendingRequest(requestId);
        requireMayDecide(caller, request);
        recentAuthenticationGuard.requireAuthenticatedWithin(properties.getRecentLogin());
        final WmpRole role = parseRole(request.getRole());
        if (assignmentRepository.existsByUserIdAndBankCodeAndBranchCodeAndRole(
                request.getUserId(), request.getBankCode(), request.getBranchCode(), role.name())) {
            throw alreadyExists(request.getUserId(), role, request.getBankCode(), request.getBranchCode());
        }
        // the branch may have been closed since the request
        bankBranchValidator.requireActiveBranch(request.getBankCode(), request.getBranchCode());
        final KnownUserEntity target = knownUserRepository.findById(request.getUserId())
                .orElseThrow(GenericObjectNotFoundException.with(String.valueOf(request.getUserId())));
        decide(request, RoleAssignmentRequestStatus.APPROVED, caller, null);
        final RoleAssignmentEntity assignment = saveAssignment(target, request.getBankCode(), request.getBranchCode(),
                role, request.getRequestedBy(), caller.getEmail(), request.getId());
        return mapper.toRoleAssignment(assignment);
    }

    @Transactional(noRollbackFor = ResourceConflictException.class)
    public RoleAssignmentRequest reject(Long requestId, String reason) {
        final WMPContext caller = SecurityContextHolder.getContextOrThrow();
        final RoleAssignmentRequestEntity request = loadPendingRequest(requestId);
        requireMayDecide(caller, request);
        recentAuthenticationGuard.requireAuthenticatedWithin(properties.getRecentLogin());
        decide(request, RoleAssignmentRequestStatus.REJECTED, caller, reason);
        return mapper.toRoleAssignmentRequest(request);
    }

    @Transactional
    public void revoke(Long assignmentId) {
        final WMPContext caller = SecurityContextHolder.getContextOrThrow();
        final RoleAssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(GenericObjectNotFoundException.with(String.valueOf(assignmentId)));
        if (isSamePerson(caller, assignment.getUserId())) {
            throw AssignmentDenied.because("nobody can revoke their own role");
        }
        // an unknown stored role is treated as BANK_ADMIN level: only a PLATFORM_ADMIN may remove it
        final WmpRole role = WmpRole.parse(assignment.getRole()).orElse(WmpRole.BANK_ADMIN);
        final AssignmentPolicy policy = policyDispatcher.forCaller(caller);
        if (!policy.mayRevoke(caller, role, assignment.getBankCode(), assignment.getBranchCode())) {
            throw AssignmentDenied.because("the caller may not revoke a " + role + " assignment there");
        }
        if (policy.requiresRecentLoginToRevoke()) {
            recentAuthenticationGuard.requireAuthenticatedWithin(properties.getRecentLogin());
        }
        assignmentRepository.delete(assignment);
        // inside the transaction: if the token store is down nothing is revoked and the caller gets 503
        tokenRevocationService.revokeAllIssuedUpTo(assignment.getUserId().toString(), clock.instant().getEpochSecond());
        applicationEventPublisher.publishEvent(new RoleAssignmentRevokedEvent()
                .assignmentId(assignment.getId())
                .userId(assignment.getUserId())
                .email(assignment.getUserEmail())
                .bankCode(assignment.getBankCode())
                .branchCode(assignment.getBranchCode())
                .role(assignment.getRole())
                .revokedBy(caller.getEmail()));
        log.info("Role assignment [{}] revoked: role [{}] in [{}/{}] for userId [{}]; sessions ended",
                assignment.getId(), role, assignment.getBankCode(), assignment.getBranchCode(), assignment.getUserId());
    }

    @Transactional(readOnly = true)
    public List<RoleAssignment> listAssignments(UUID userId) {
        final WMPContext caller = SecurityContextHolder.getContextOrThrow();
        final AssignmentPolicy policy = policyDispatcher.forCaller(caller);
        return assignmentRepository.findByUserIdOrderByIdAsc(userId).stream()
                .filter(a -> policy.mayView(caller, a.getBankCode(), a.getBranchCode()))
                .map(mapper::toRoleAssignment)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleAssignmentRequest> listRequests(RoleAssignmentRequestStatus status) {
        final WMPContext caller = SecurityContextHolder.getContextOrThrow();
        final WmpRole callerRole = policyDispatcher.forCaller(caller).callerRole();
        final List<RoleAssignmentRequestEntity> requests;
        if (callerRole == WmpRole.PLATFORM_ADMIN) {
            requests = status == null
                    ? requestRepository.findAllByOrderByRequestedAtDesc()
                    : requestRepository.findByStatusOrderByRequestedAtDesc(status);
        } else if (callerRole == WmpRole.BANK_ADMIN) {
            requests = status == null
                    ? requestRepository.findByBankCodeOrderByRequestedAtDesc(caller.getTenantId())
                    : requestRepository.findByBankCodeAndStatusOrderByRequestedAtDesc(caller.getTenantId(), status);
        } else {
            throw AssignmentDenied.because("only BANK_ADMIN and PLATFORM_ADMIN can list requests");
        }
        return requests.stream().map(mapper::toRoleAssignmentRequest).toList();
    }

    /**
     * First platform run: gives PLATFORM_ADMIN to a configured user. No caller, no policy: called from the
     * user-created consumer. Idempotent.
     */
    @Transactional
    public void bootstrapPlatformAdmin(KnownUserEntity user) {
        if (assignmentRepository.existsByUserIdAndBankCodeAndBranchCodeAndRole(user.getUserId(),
                AssignmentConstants.PLATFORM_TENANT,
                AssignmentConstants.PLATFORM_TENANT,
                WmpRole.PLATFORM_ADMIN.name())) {
            log.info("User [{}] is already a PLATFORM_ADMIN, bootstrap skipped", user.getEmail());
            return;
        }
        saveAssignment(user,
                AssignmentConstants.PLATFORM_TENANT,
                AssignmentConstants.PLATFORM_TENANT,
                WmpRole.PLATFORM_ADMIN,
                AssignmentConstants.BOOTSTRAP_ACTOR, null, null);
        log.info("Bootstrap: PLATFORM_ADMIN assigned to [{}]", user.getEmail());
    }

    private RoleAssignmentEntity saveAssignment(
            KnownUserEntity user, String bankCode, String branchCode, WmpRole role,
            String grantedBy, String approvedBy, Long requestId) {
        final RoleAssignmentEntity assignment = assignmentRepository.save(RoleAssignmentEntity.builder()
                .userId(user.getUserId())
                .userEmail(user.getEmail())
                .bankCode(bankCode)
                .branchCode(branchCode)
                .role(role.name())
                .grantedBy(grantedBy)
                .approvedBy(approvedBy)
                .requestId(requestId)
                .build());
        applicationEventPublisher.publishEvent(new RoleAssignmentGrantedEvent()
                .assignmentId(assignment.getId())
                .userId(user.getUserId())
                .email(user.getEmail())
                .bankCode(bankCode)
                .branchCode(branchCode)
                .role(role.name())
                .grantedBy(grantedBy)
                .approvedBy(approvedBy));
        log.info("Role assignment [{}] created: role [{}] in [{}/{}] for userId [{}]",
                assignment.getId(), role, bankCode, branchCode, user.getUserId());
        return assignment;
    }

    private RoleAssignmentRequestEntity loadPendingRequest(Long requestId) {
        final RoleAssignmentRequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(GenericObjectNotFoundException.with(String.valueOf(requestId)));
        if (request.getStatus() == RoleAssignmentRequestStatus.PENDING
                && request.getRequestedAt().plus(properties.getRequestTtl()).isBefore(clock.instant())) {
            // lazy expiry: there is no scheduler yet
            request.setStatus(RoleAssignmentRequestStatus.EXPIRED);
            request.setDecidedAt(clock.instant());
            log.info("Role assignment request [{}] expired", requestId);
        }
        if (request.getStatus() != RoleAssignmentRequestStatus.PENDING) {
            throw ResourceConflictException.of(ErrorCode.ROLE_ASSIGNMENT_REQUEST_NOT_PENDING, requestId, request.getStatus());
        }
        return request;
    }

    private void requireMayDecide(WMPContext caller, RoleAssignmentRequestEntity request) {
        final WmpRole role = parseRole(request.getRole());
        if (!policyDispatcher.forCaller(caller).mayDecide(caller, role, request.getBankCode())) {
            throw AssignmentDenied.because("the caller may not decide a " + role + " request for " + request.getBankCode());
        }
        // four-eyes: the second pair of eyes belongs to someone else
        if (Objects.equals(caller.getUserId(), request.getRequestedByUserId())) {
            throw AssignmentDenied.because("the requester can't decide its own request");
        }
        if (isSamePerson(caller, request.getUserId())) {
            throw AssignmentDenied.because("nobody can decide a request about themselves");
        }
    }

    private void decide(RoleAssignmentRequestEntity request, RoleAssignmentRequestStatus status, WMPContext caller, String reason) {
        request.setStatus(status);
        request.setDecidedBy(caller.getEmail());
        request.setDecidedAt(clock.instant());
        request.setReason(reason);
        log.info("Role assignment request [{}] {} by [{}]", request.getId(), status, caller.getEmail());
    }

    private void requireNoAssignmentOrPendingRequest(UUID userId, String bankCode, String branchCode, WmpRole role) {
        if (assignmentRepository.existsByUserIdAndBankCodeAndBranchCodeAndRole(userId, bankCode, branchCode, role.name())
                || requestRepository.existsByUserIdAndBankCodeAndBranchCodeAndRoleAndStatus(
                        userId, bankCode, branchCode, role.name(), RoleAssignmentRequestStatus.PENDING)) {
            throw alreadyExists(userId, role, bankCode, branchCode);
        }
    }

    private static ResourceConflictException alreadyExists(UUID userId, WmpRole role, String bankCode, String branchCode) {
        return ResourceConflictException.of(ErrorCode.ROLE_ASSIGNMENT_ALREADY_EXISTS, userId, role, bankCode, branchCode);
    }

    private static boolean isSamePerson(WMPContext caller, UUID userId) {
        return userId != null && Objects.equals(caller.getUserId(), userId.toString());
    }

    private static WmpRole parseRole(String value) {
        return WmpRole.parse(value).orElseThrow(() -> new RequestValidationException(ErrorCode.REQUEST_VALIDATION_ERROR,
                String.format(ErrorCode.REQUEST_VALIDATION_ERROR.getMessageTemplate(), "role", value)));
    }
}
