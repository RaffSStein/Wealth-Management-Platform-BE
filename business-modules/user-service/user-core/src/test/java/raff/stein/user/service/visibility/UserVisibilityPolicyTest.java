package raff.stein.user.service.visibility;

import org.junit.jupiter.api.Test;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.user.model.entity.BranchUserEntity;
import raff.stein.user.model.entity.UserEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserVisibilityPolicyTest {

    private final UserVisibilityPolicy policy = new UserVisibilityPolicy();

    private final UserEntity unlinked = user();
    private final UserEntity inBr01 = user(assignment("BANK_A", "BR_01", "CUSTOMER"));
    private final UserEntity inBr02 = user(assignment("BANK_A", "BR_02", "CUSTOMER"));
    private final UserEntity inBankB = user(assignment("BANK_B", "BR_09", "CUSTOMER"));

    @Test
    void customerShouldSeeOnlyItself() {
        final WMPContext customer = caller(inBr01.getId(), "CUSTOMER", "BANK_A", "BR_01");

        assertTrue(policy.canSee(customer, inBr01));
        assertFalse(policy.canSee(customer, unlinked));
        assertFalse(policy.canSee(customer, inBr02));
    }

    @Test
    void advisorShouldSeeItsBranchButNotUnlinkedUsers() {
        final WMPContext advisor = caller(UUID.randomUUID(), "ADVISOR", "BANK_A", "BR_01");

        assertTrue(policy.canSee(advisor, inBr01));
        assertFalse(policy.canSee(advisor, inBr02));
        assertFalse(policy.canSee(advisor, inBankB));
        assertFalse(policy.canSee(advisor, unlinked));
    }

    @Test
    void bankAdminShouldSeeItsBankAndUnlinkedUsers() {
        final WMPContext bankAdmin = caller(UUID.randomUUID(), "BANK_ADMIN", "BANK_A", "BR_01");

        assertTrue(policy.canSee(bankAdmin, inBr01));
        assertTrue(policy.canSee(bankAdmin, inBr02));
        assertTrue(policy.canSee(bankAdmin, unlinked));
        assertFalse(policy.canSee(bankAdmin, inBankB));
    }

    @Test
    void platformAdminShouldSeeEveryone() {
        final WMPContext platformAdmin = caller(UUID.randomUUID(), "PLATFORM_ADMIN", "PLATFORM", "PLATFORM");

        for (UserEntity user : List.of(unlinked, inBr01, inBr02, inBankB)) {
            assertTrue(policy.canSee(platformAdmin, user));
        }
    }

    @Test
    void shouldShowOnlyTheAssignmentsInTheCallersScope() {
        final BranchUserEntity bankA = assignment("BANK_A", "BR_01", "ADVISOR");
        final BranchUserEntity bankB = assignment("BANK_B", "BR_09", "CUSTOMER");
        final UserEntity twoBanks = user(bankA, bankB);
        final WMPContext bankAdmin = caller(UUID.randomUUID(), "BANK_ADMIN", "BANK_A", "BR_01");

        assertTrue(policy.canSeeAssignment(bankAdmin, twoBanks, bankA));
        assertFalse(policy.canSeeAssignment(bankAdmin, twoBanks, bankB));
        assertTrue(policy.canSeeAssignment(caller(twoBanks.getId(), "ADVISOR", "BANK_A", "BR_01"), twoBanks, bankB));
    }

    @Test
    void shouldUseTheMostPrivilegedRoleOfTheToken() {
        final WMPContext both = WMPContext.builder().userId(UUID.randomUUID().toString())
                .roles(Set.of("ADVISOR", "BANK_ADMIN")).tenantId("BANK_A").branchCode("BR_01").build();

        assertTrue(policy.canSee(both, unlinked));
    }

    private static WMPContext caller(UUID userId, String role, String bankCode, String branchCode) {
        return WMPContext.builder().userId(userId.toString()).roles(Set.of(role))
                .tenantId(bankCode).branchCode(branchCode).build();
    }

    private static UserEntity user(BranchUserEntity... assignments) {
        final UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID()).email("u@bank.test").bankBranchUsers(new HashSet<>(Set.of(assignments))).build();
        return user;
    }

    private static BranchUserEntity assignment(String bankCode, String branchCode, String role) {
        return BranchUserEntity.builder().bankCode(bankCode).branchCode(branchCode).role(role).build();
    }
}
