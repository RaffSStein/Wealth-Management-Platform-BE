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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerAccessPolicyTest {

    private static final UUID CALLER_ID = UUID.randomUUID();
    private static final String CALLER_EMAIL = "Caller@Bank.test";
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID OWN_CUSTOMER_ID = UUID.randomUUID();
    private static final UUID FOREIGN_CUSTOMER_ID = UUID.randomUUID();
    private static final UUID BRANCH_CUSTOMER_ID = UUID.randomUUID();

    private CustomerAccessPolicy policy;

    @BeforeEach
    void setUp() {
        final CustomerRepository customerRepository = mock(CustomerRepository.class);
        when(customerRepository.existsByIdAndUserId(OWN_CUSTOMER_ID, CALLER_ID)).thenReturn(true);
        when(customerRepository.existsByIdAndBranchCode(BRANCH_CUSTOMER_ID, "BR_01")).thenReturn(true);
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
                .branchCode("BR_01")
                .email(CALLER_EMAIL)
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
    void shouldLimitAdvisorToTheBranchInItsToken() {
        signInAs("ADVISOR");

        assertDoesNotThrow(() -> policy.assertCanRead(BRANCH_CUSTOMER_ID));
        assertDoesNotThrow(() -> policy.assertCanWrite(BRANCH_CUSTOMER_ID));
        assertThrows(CustomerNotFoundException.class, () -> policy.assertCanRead(FOREIGN_CUSTOMER_ID));
        assertThrows(CustomerNotFoundException.class, () -> policy.assertCanWrite(FOREIGN_CUSTOMER_ID));
        assertDoesNotThrow(() -> policy.assertCanList());
        assertDoesNotThrow(() -> policy.assertCanReadAdvisorDashboard());
        assertEquals(Optional.of("BR_01"), policy.branchScope());
        assertFalse(policy.isLimitedToOneRecord());
    }

    @Test
    void shouldStartAdvisorRecordUnlinkedWhateverTheBodySays() {
        signInAs("ADVISOR");

        assertNull(policy.resolveInitUserId(OTHER_USER_ID));
        assertNull(policy.resolveInitUserId(null));
        assertEquals("BR_01", policy.resolveInitBranchCode());
    }

    @Test
    void shouldLetAdvisorWithoutBranchReachNothing() {
        SecurityContextHolder.setContext(WMPContext.builder()
                .userId(CALLER_ID.toString())
                .roles(Set.of("ADVISOR"))
                .tenantId("BANK_A")
                .build());

        assertThrows(CustomerNotFoundException.class, () -> policy.assertCanRead(BRANCH_CUSTOMER_ID));
        assertThrows(AccessDeniedException.class, () -> policy.assertCanList());
        assertThrows(AccessDeniedException.class, () -> policy.branchScope());
        assertThrows(AccessDeniedException.class, () -> policy.resolveInitBranchCode());
        assertThrows(AccessDeniedException.class, () -> policy.assertCanReadAdvisorDashboard());
    }

    @Test
    void shouldRequireAClientEmailFromAdvisor() {
        signInAs("ADVISOR");

        assertEquals("client@bank.test", policy.resolveInitEmail(" Client@Bank.test "));
        assertThrows(RequestValidationException.class, () -> policy.resolveInitEmail(null));
        assertThrows(RequestValidationException.class, () -> policy.resolveInitEmail(" "));
        // whatever the case: a role on a client's record must not end up on the advisor
        assertThrows(AccessDeniedException.class, () -> policy.resolveInitEmail("caller@bank.TEST"));
    }

    @Test
    void shouldTakeCustomerEmailFromToken() {
        signInAs("CUSTOMER");

        assertEquals("caller@bank.test", policy.resolveInitEmail("someone.else@bank.test"));
        assertEquals("caller@bank.test", policy.resolveInitEmail(null));
    }

    @Test
    void shouldRefuseListAndDashboardToCustomer() {
        signInAs("CUSTOMER");

        assertThrows(AccessDeniedException.class, () -> policy.assertCanList());
        assertThrows(AccessDeniedException.class, () -> policy.assertCanReadAdvisorDashboard());
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
        assertDoesNotThrow(() -> policy.assertCanList());
        assertTrue(policy.branchScope().isEmpty());
        assertThrows(AccessDeniedException.class, () -> policy.assertCanReadAdvisorDashboard());
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
