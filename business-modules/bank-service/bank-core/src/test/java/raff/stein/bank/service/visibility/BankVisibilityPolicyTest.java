package raff.stein.bank.service.visibility;

import org.junit.jupiter.api.Test;
import raff.stein.platformcore.security.context.WMPContext;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankVisibilityPolicyTest {

    private final BankVisibilityPolicy policy = new BankVisibilityPolicy();

    @Test
    void platformAdminShouldSeeEveryBank() {
        final BankScope scope = policy.scopeOf(caller("PLATFORM_ADMIN", "PLATFORM"));

        assertTrue(scope.all());
        assertTrue(scope.includes("BANK_A"));
        assertTrue(scope.includes("BANK_B"));
    }

    @Test
    void otherRolesShouldSeeOnlyTheBankOfTheirToken() {
        for (String role : Set.of("BANK_ADMIN", "ADVISOR", "CUSTOMER")) {
            final BankScope scope = policy.scopeOf(caller(role, "BANK_A"));

            assertFalse(scope.all(), role);
            assertTrue(scope.includes("BANK_A"), role);
            assertFalse(scope.includes("BANK_B"), role);
        }
    }

    @Test
    void aTokenWithoutBankShouldSeeNothing() {
        assertTrue(policy.scopeOf(caller("CUSTOMER", null)).isNone());
        assertTrue(policy.scopeOf(null).isNone());
    }

    @Test
    void anUnknownRoleShouldNotGrantEveryBank() {
        final BankScope scope = policy.scopeOf(caller("SOMETHING", "BANK_A"));

        assertFalse(scope.all());
        assertFalse(scope.includes("BANK_B"));
    }

    private static WMPContext caller(String role, String bankCode) {
        return WMPContext.builder().userId("u-1").roles(Set.of(role)).tenantId(bankCode).branchCode("BR_01").build();
    }
}
