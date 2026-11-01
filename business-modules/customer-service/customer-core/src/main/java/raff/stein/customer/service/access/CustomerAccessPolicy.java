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

import java.util.Optional;
import java.util.UUID;

/**
 * Who may read or change which customer. The role is the active one in the token, so a user who is a customer in one
 * branch and an advisor in another gets the rule of the membership it is signed in with.
 * <ul>
 *     <li>CUSTOMER: only its own record; anything else is answered like "not found"</li>
 *     <li>ADVISOR: reads and writes any customer in its bank</li>
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
        final WmpRole role = callerRole();
        if (role == WmpRole.CUSTOMER) {
            assertOwnRecord(customerId);
        } else if (role == WmpRole.BANK_ADMIN || role == WmpRole.PLATFORM_ADMIN) {
            log.info("Admin read of customer data, customerId: [{}], callerId: [{}]", customerId, callerId());
        }
    }

    public void assertCanWrite(UUID customerId) {
        switch (callerRole()) {
            case CUSTOMER -> assertOwnRecord(customerId);
            case ADVISOR -> { /* any customer in its bank */ }
            case BANK_ADMIN, PLATFORM_ADMIN -> throw denied(customerId);
        }
    }

    public void assertCanListByUser(UUID userId) {
        if (callerRole() == WmpRole.CUSTOMER && !userId.equals(callerUserId().orElse(null))) {
            throw GenericObjectNotFoundException.with(userId.toString()).get();
        }
    }

    /**
     * The user a new customer record belongs to. A customer can only create its own, whatever the body says.
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
            case ADVISOR -> {
                if (requestedUserId == null) {
                    RequestValidationException.of(ErrorCode.REQUEST_VALIDATION_ERROR, "userId", "null");
                }
                yield requestedUserId;
            }
            case BANK_ADMIN, PLATFORM_ADMIN -> throw denied("a customer record");
        };
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

    private static AccessDeniedException denied(Object target) {
        return new AccessDeniedException(
                ErrorCode.ACCESS_DENIED,
                String.format(ErrorCode.ACCESS_DENIED.getMessageTemplate(), callerId(), target));
    }

    private static WmpRole callerRole() {
        final WMPContext caller = SecurityContextHolder.getContext();
        return WmpRole.highestOf(caller != null ? caller.getRoles() : null);
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
