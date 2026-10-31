package raff.stein.profiler.service.assignment.policy;

import org.junit.jupiter.api.Test;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.role.WmpRole;
import raff.stein.profiler.model.assignment.AssignRoleCommand;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentPolicyTest {

    private final AdvisorAssignmentPolicy advisor = new AdvisorAssignmentPolicy();
    private final BankAdminAssignmentPolicy bankAdmin = new BankAdminAssignmentPolicy();
    private final PlatformAdminAssignmentPolicy platformAdmin = new PlatformAdminAssignmentPolicy();

    @Test
    void advisorShouldAssignCustomerInOwnBranchDirectly() {
        final AssignmentDecision decision = advisor.decideAssignment(
                caller("ADVISOR", "BANK_A", "BR_01"), WmpRole.CUSTOMER, command("BR_01", null));

        assertEquals("BANK_A", decision.bankCode());
        assertEquals("BR_01", decision.branchCode());
        assertFalse(decision.requiresApproval());
        assertFalse(decision.requiresRecentLogin());
    }

    @Test
    void advisorShouldNotAssignStaffOrOtherBranchOrOtherBank() {
        final WMPContext caller = caller("ADVISOR", "BANK_A", "BR_01");

        assertThrows(AccessDeniedException.class, () -> advisor.decideAssignment(caller, WmpRole.ADVISOR, command("BR_01", null)));
        assertThrows(AccessDeniedException.class, () -> advisor.decideAssignment(caller, WmpRole.CUSTOMER, command("BR_02", null)));
        assertThrows(AccessDeniedException.class, () -> advisor.decideAssignment(caller, WmpRole.CUSTOMER, command("BR_01", "BANK_B")));
    }

    @Test
    void bankAdminShouldAssignCustomerDirectlyAndAdvisorWithApproval() {
        final WMPContext caller = caller("BANK_ADMIN", "BANK_A", "BR_01");

        final AssignmentDecision customer = bankAdmin.decideAssignment(caller, WmpRole.CUSTOMER, command("BR_07", null));
        final AssignmentDecision staff = bankAdmin.decideAssignment(caller, WmpRole.ADVISOR, command("BR_07", null));

        assertEquals("BANK_A", customer.bankCode());
        assertFalse(customer.requiresApproval());
        assertTrue(staff.requiresApproval());
        assertTrue(staff.requiresRecentLogin());
    }

    @Test
    void bankAdminShouldNotAssignBankAdminOrChooseTheBank() {
        final WMPContext caller = caller("BANK_ADMIN", "BANK_A", "BR_01");

        assertThrows(AccessDeniedException.class, () -> bankAdmin.decideAssignment(caller, WmpRole.BANK_ADMIN, command("BR_01", null)));
        assertThrows(AccessDeniedException.class, () -> bankAdmin.decideAssignment(caller, WmpRole.ADVISOR, command("BR_01", "BANK_B")));
    }

    @Test
    void platformAdminShouldAssignBankAdminOfANamedBankWithApproval() {
        final AssignmentDecision decision = platformAdmin.decideAssignment(
                caller("PLATFORM_ADMIN", "PLATFORM", "PLATFORM"), WmpRole.BANK_ADMIN, command("BR_01", "BANK_A"));

        assertEquals("BANK_A", decision.bankCode());
        assertTrue(decision.requiresApproval());
        assertTrue(decision.requiresRecentLogin());
    }

    @Test
    void platformAdminShouldOnlyAssignBankAdminInARealBank() {
        final WMPContext caller = caller("PLATFORM_ADMIN", "PLATFORM", "PLATFORM");

        assertThrows(AccessDeniedException.class, () -> platformAdmin.decideAssignment(caller, WmpRole.ADVISOR, command("BR_01", "BANK_A")));
        assertThrows(RequestValidationException.class, () -> platformAdmin.decideAssignment(caller, WmpRole.BANK_ADMIN, command("BR_01", null)));
        assertThrows(RequestValidationException.class, () -> platformAdmin.decideAssignment(caller, WmpRole.BANK_ADMIN, command("PLATFORM", "PLATFORM")));
    }

    @Test
    void shouldLetOnlyTheRightAdminDecideRequests() {
        assertTrue(bankAdmin.mayDecide(caller("BANK_ADMIN", "BANK_A", "BR_01"), WmpRole.ADVISOR, "BANK_A"));
        assertFalse(bankAdmin.mayDecide(caller("BANK_ADMIN", "BANK_B", "BR_09"), WmpRole.ADVISOR, "BANK_A"));
        assertFalse(bankAdmin.mayDecide(caller("BANK_ADMIN", "BANK_A", "BR_01"), WmpRole.BANK_ADMIN, "BANK_A"));
        assertTrue(platformAdmin.mayDecide(caller("PLATFORM_ADMIN", "PLATFORM", "PLATFORM"), WmpRole.BANK_ADMIN, "BANK_A"));
        assertFalse(advisor.mayDecide(caller("ADVISOR", "BANK_A", "BR_01"), WmpRole.CUSTOMER, "BANK_A"));
    }

    @Test
    void shouldRevokeOnlyWithinTheCallersScope() {
        assertTrue(advisor.mayRevoke(caller("ADVISOR", "BANK_A", "BR_01"), WmpRole.CUSTOMER, "BANK_A", "BR_01"));
        assertFalse(advisor.mayRevoke(caller("ADVISOR", "BANK_A", "BR_01"), WmpRole.CUSTOMER, "BANK_A", "BR_02"));
        assertTrue(bankAdmin.mayRevoke(caller("BANK_ADMIN", "BANK_A", "BR_01"), WmpRole.ADVISOR, "BANK_A", "BR_02"));
        assertFalse(bankAdmin.mayRevoke(caller("BANK_ADMIN", "BANK_A", "BR_01"), WmpRole.ADVISOR, "BANK_B", "BR_09"));
        assertFalse(bankAdmin.mayRevoke(caller("BANK_ADMIN", "BANK_A", "BR_01"), WmpRole.BANK_ADMIN, "BANK_A", "BR_01"));
        assertTrue(platformAdmin.mayRevoke(caller("PLATFORM_ADMIN", "PLATFORM", "PLATFORM"), WmpRole.BANK_ADMIN, "BANK_A", "BR_01"));
    }

    @Test
    void dispatcherShouldPickTheMostPrivilegedRoleAndRefuseCustomers() {
        final AssignmentPolicyDispatcher dispatcher = new AssignmentPolicyDispatcher(List.of(advisor, bankAdmin, platformAdmin));

        assertEquals(WmpRole.BANK_ADMIN, dispatcher.forCaller(caller(Set.of("ADVISOR", "BANK_ADMIN"))).callerRole());
        assertThrows(AccessDeniedException.class, () -> dispatcher.forCaller(caller(Set.of("CUSTOMER"))));
        assertThrows(AccessDeniedException.class, () -> dispatcher.forCaller(caller(Set.of("SOMETHING"))));
    }

    private static WMPContext caller(String role, String bankCode, String branchCode) {
        return WMPContext.builder().userId("caller-1").email("caller@bank.test")
                .roles(Set.of(role)).tenantId(bankCode).branchCode(branchCode).build();
    }

    private static WMPContext caller(Set<String> roles) {
        return WMPContext.builder().userId("caller-1").roles(roles).tenantId("BANK_A").branchCode("BR_01").build();
    }

    private static AssignRoleCommand command(String branchCode, String bankCode) {
        return AssignRoleCommand.builder().branchCode(branchCode).bankCode(bankCode).build();
    }
}
