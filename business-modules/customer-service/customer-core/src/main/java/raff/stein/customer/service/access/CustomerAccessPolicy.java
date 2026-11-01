package raff.stein.customer.service.access;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.customer.exception.CustomerNotFoundException;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.role.WmpRole;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Who may read or change which customer. The role is the active one in the token, so a user who is a customer in one
 * branch and an advisor in another gets the rule of the membership it is signed in with.
 * <ul>
 *     <li>CUSTOMER: only its own record; anything else is answered like "not found"</li>
 *     <li>ADVISOR: reads and writes the customers of the branch in its token; anything else is answered like "not found"</li>
 *     <li>BANK_ADMIN: reads any customer in its bank, no writes</li>
 *     <li>PLATFORM_ADMIN: reads any customer in every bank, no writes</li>
 * </ul>
 * The bank boundary itself comes from the Hibernate tenant filter. Endpoints reach it through
 * {@link raff.stein.customer.controller.access.CustomerAccess}, not from the services, because Kafka consumers use the
 * same services without a user.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerAccessPolicy {

    private final CustomerRepository customerRepository;

    public void assertCanRead(UUID customerId) {
        switch (callerRole()) {
            case CUSTOMER -> assertOwnRecord(customerId);
            case ADVISOR -> assertInCallerBranch(customerId);
            case BANK_ADMIN, PLATFORM_ADMIN ->
                    log.info("Admin read of customer data, customerId: [{}], callerId: [{}]", customerId, callerId());
        }
    }

    public void assertCanWrite(UUID customerId) {
        switch (callerRole()) {
            case CUSTOMER -> assertOwnRecord(customerId);
            case ADVISOR -> assertInCallerBranch(customerId);
            case BANK_ADMIN, PLATFORM_ADMIN -> throw denied(customerId);
        }
    }

    /**
     * Listing is for staff: a customer never sees other customers. An advisor token with no branch reaches nothing.
     */
    public void assertCanList() {
        final WmpRole role = callerRole();
        if (role == WmpRole.CUSTOMER || (role == WmpRole.ADVISOR && callerBranch().isEmpty())) {
            throw denied("the customer list");
        }
    }

    public void assertCanReadAdvisorDashboard() {
        if (callerRole() != WmpRole.ADVISOR || callerBranch().isEmpty()) {
            throw denied("the advisor dashboard");
        }
    }

    /**
     * The branch a caller's reads are limited to, empty when it isn't limited to one (the bank boundary still applies).
     */
    public Optional<String> branchScope() {
        if (callerRole() != WmpRole.ADVISOR) {
            return Optional.empty();
        }
        return Optional.of(callerBranch().orElseThrow(() -> denied("the customers of a branch")));
    }

    /**
     * The branch of a new customer record: always the one in the token.
     */
    public String resolveInitBranchCode() {
        return callerBranch().orElseThrow(() -> denied("a customer record"));
    }

    public void assertCanListByUser(UUID userId) {
        if (callerRole() == WmpRole.CUSTOMER && !userId.equals(callerUserId().orElse(null))) {
            throw GenericObjectNotFoundException.with(userId.toString()).get();
        }
    }

    /**
     * The user a new customer record belongs to. A customer can only create its own, whatever the body says. An
     * advisor's record starts with no user: only the role assignment events link it, so the body can't attach anyone.
     */
    public UUID resolveInitUserId(UUID requestedUserId) {
        return switch (callerRole()) {
            case CUSTOMER -> {
                final UUID ownId = callerUserId().orElseThrow(() -> denied("a customer record"));
                if (requestedUserId != null && !requestedUserId.equals(ownId)) {
                    throw denied(requestedUserId);
                }
                yield ownId;
            }
            case ADVISOR -> null;
            case BANK_ADMIN, PLATFORM_ADMIN -> throw denied("a customer record");
        };
    }

    /**
     * The email of a new customer record, lowercase. A customer's comes from its token. An advisor must send the client's,
     * and can't use its own: that would give it the CUSTOMER role on a client's record.
     */
    public String resolveInitEmail(String requestedEmail) {
        final String callerEmail = callerEmail().orElse(null);
        if (callerRole() != WmpRole.ADVISOR) {
            return Optional.ofNullable(callerEmail).orElseThrow(() -> denied("a customer record"));
        }
        if (requestedEmail == null || requestedEmail.isBlank()) {
            RequestValidationException.of(ErrorCode.REQUEST_VALIDATION_ERROR, "email", String.valueOf(requestedEmail));
        }
        final String email = requestedEmail.trim().toLowerCase(Locale.ROOT);
        if (email.equals(callerEmail)) {
            throw denied(email);
        }
        return email;
    }

    public boolean isLimitedToOneRecord() {
        return callerRole() == WmpRole.CUSTOMER;
    }

    private void assertOwnRecord(UUID customerId) {
        final boolean own = callerUserId()
                .map(userId -> customerRepository.existsByIdAndUserId(customerId, userId))
                .orElse(false);
        if (!own) {
            throw CustomerNotFoundException.with(customerId).get();
        }
    }

    private void assertInCallerBranch(UUID customerId) {
        final boolean inBranch = callerBranch()
                .map(branchCode -> customerRepository.existsByIdAndBranchCode(customerId, branchCode))
                .orElse(false);
        if (!inBranch) {
            throw CustomerNotFoundException.with(customerId).get();
        }
    }

    private static AccessDeniedException denied(Object target) {
        return new AccessDeniedException(
                ErrorCode.ACCESS_DENIED,
                String.format(ErrorCode.ACCESS_DENIED.getMessageTemplate(), callerId(), target));
    }

    private static WmpRole callerRole() {
        final WMPContext caller = SecurityContextHolder.getContext();
        return WmpRole.highestOf(caller != null ? caller.getRoles() : null);
    }

    private static Optional<String> callerBranch() {
        final WMPContext caller = SecurityContextHolder.getContext();
        return Optional.ofNullable(caller != null ? caller.getBranchCode() : null).filter(branch -> !branch.isBlank());
    }

    private static Optional<String> callerEmail() {
        final WMPContext caller = SecurityContextHolder.getContext();
        return Optional.ofNullable(caller != null ? caller.getEmail() : null)
                .filter(email -> !email.isBlank())
                .map(email -> email.toLowerCase(Locale.ROOT));
    }

    private static String callerId() {
        final WMPContext caller = SecurityContextHolder.getContext();
        return caller != null ? caller.getUserId() : null;
    }

    private static Optional<UUID> callerUserId() {
        try {
            return Optional.ofNullable(callerId()).map(UUID::fromString);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
