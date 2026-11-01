package raff.stein.customer.service.access;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.customer.exception.CustomerNotFoundException;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerAccessPolicyTest {

    private static final UUID CALLER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID OWN_CUSTOMER_ID = UUID.randomUUID();
    private static final UUID FOREIGN_CUSTOMER_ID = UUID.randomUUID();

    private CustomerAccessPolicy policy;

    @BeforeEach
    void setUp() {
        final CustomerRepository customerRepository = mock(CustomerRepository.class);
        when(customerRepository.existsByIdAndUserId(OWN_CUSTOMER_ID, CALLER_ID)).thenReturn(true);
        policy = new CustomerAccessPolicy(customerRepository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    private static void signInAs(String role) {
        SecurityContextHolder.setContext(WMPContext.builder()
                .userId(CALLER_ID.toString())
                .roles(Set.of(role))
                .tenantId("BANK_A")
                .build());
    }

    @Test
    void shouldLetCustomerReachOnlyItsOwnRecord() {
        signInAs("CUSTOMER");

        assertDoesNotThrow(() -> policy.assertCanRead(OWN_CUSTOMER_ID));
        assertDoesNotThrow(() -> policy.assertCanWrite(OWN_CUSTOMER_ID));
        assertThrows(CustomerNotFoundException.class, () -> policy.assertCanRead(FOREIGN_CUSTOMER_ID));
        assertThrows(CustomerNotFoundException.class, () -> policy.assertCanWrite(FOREIGN_CUSTOMER_ID));
    }

    @Test
    void shouldLetCustomerListOnlyItsOwnUser() {
        signInAs("CUSTOMER");

        assertDoesNotThrow(() -> policy.assertCanListByUser(CALLER_ID));
        assertThrows(GenericObjectNotFoundException.class, () -> policy.assertCanListByUser(OTHER_USER_ID));
    }

    @Test
    void shouldTakeInitUserIdFromTokenForCustomer() {
        signInAs("CUSTOMER");

        assertEquals(CALLER_ID, policy.resolveInitUserId(null));
        assertEquals(CALLER_ID, policy.resolveInitUserId(CALLER_ID));
        assertThrows(AccessDeniedException.class, () -> policy.resolveInitUserId(OTHER_USER_ID));
        assertTrue(policy.isLimitedToOneRecord());
    }

    @Test
    void shouldLeaveAdvisorUnchanged() {
        signInAs("ADVISOR");

        assertDoesNotThrow(() -> policy.assertCanRead(FOREIGN_CUSTOMER_ID));
        assertDoesNotThrow(() -> policy.assertCanWrite(FOREIGN_CUSTOMER_ID));
        assertDoesNotThrow(() -> policy.assertCanListByUser(OTHER_USER_ID));
        assertEquals(OTHER_USER_ID, policy.resolveInitUserId(OTHER_USER_ID));
        assertThrows(RequestValidationException.class, () -> policy.resolveInitUserId(null));
        assertFalse(policy.isLimitedToOneRecord());
    }

    @Test
    void shouldKeepBankAdminReadOnly() {
        signInAs("BANK_ADMIN");
        assertAdminIsReadOnly();
    }

    @Test
    void shouldKeepPlatformAdminReadOnly() {
        signInAs("PLATFORM_ADMIN");
        assertAdminIsReadOnly();
    }

    private void assertAdminIsReadOnly() {
        assertDoesNotThrow(() -> policy.assertCanRead(FOREIGN_CUSTOMER_ID));
        assertDoesNotThrow(() -> policy.assertCanListByUser(OTHER_USER_ID));
        assertThrows(AccessDeniedException.class, () -> policy.assertCanWrite(FOREIGN_CUSTOMER_ID));
        assertThrows(AccessDeniedException.class, () -> policy.resolveInitUserId(OTHER_USER_ID));
    }

    @Test
    void shouldTreatUnknownRoleAndMissingContextAsCustomer() {
        SecurityContextHolder.setContext(WMPContext.builder()
                .userId(CALLER_ID.toString())
                .roles(Set.of("SUPERUSER"))
                .build());
        assertThrows(CustomerNotFoundException.class, () -> policy.assertCanRead(FOREIGN_CUSTOMER_ID));

        SecurityContextHolder.clear();
        assertThrows(CustomerNotFoundException.class, () -> policy.assertCanRead(OWN_CUSTOMER_ID));
    }

    @Test
    void shouldDenyCustomerWhoseTokenUserIdIsNotAUuid() {
        SecurityContextHolder.setContext(WMPContext.builder().userId("not-a-uuid").roles(Set.of("CUSTOMER")).build());

        assertThrows(CustomerNotFoundException.class, () -> policy.assertCanRead(OWN_CUSTOMER_ID));
        assertThrows(GenericObjectNotFoundException.class, () -> policy.assertCanListByUser(OTHER_USER_ID));
        assertThrows(AccessDeniedException.class, () -> policy.resolveInitUserId(null));
    }
}
