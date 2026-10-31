package raff.stein.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.user.model.entity.BranchUserEntity;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.repository.BranchUserRepository;
import raff.stein.user.repository.UserRepository;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleAssignmentReadModelServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private BranchUserRepository branchUserRepository;
    private UserRepository userRepository;
    private RoleAssignmentReadModelService service;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        branchUserRepository = mock(BranchUserRepository.class);
        userRepository = mock(UserRepository.class);
        service = new RoleAssignmentReadModelService(branchUserRepository, userRepository);
        user = UserEntity.builder().id(USER_ID).email("ada@bank.test").bankBranchUsers(new HashSet<>()).build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    @Test
    void shouldAddTheAssignmentOnce() {
        service.applyGranted(7L, USER_ID, "BANK_A", "BR_01", "ADVISOR");

        assertEquals(1, user.getBankBranchUsers().size());
        final BranchUserEntity row = user.getBankBranchUsers().iterator().next();
        assertEquals(7L, row.getAssignmentId());
        assertEquals("ADVISOR", row.getRole());
    }

    @Test
    void shouldIgnoreAReplayedGrant() {
        when(branchUserRepository.findByAssignmentId(7L)).thenReturn(Optional.of(BranchUserEntity.builder().assignmentId(7L).build()));

        service.applyGranted(7L, USER_ID, "BANK_A", "BR_01", "ADVISOR");

        assertTrue(user.getBankBranchUsers().isEmpty());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldAdoptARowCreatedBeforeRoleAssignments() {
        final BranchUserEntity legacy = BranchUserEntity.builder()
                .user(user).bankCode("BANK_A").branchCode("BR_01").role("ADVISOR").build();
        user.getBankBranchUsers().add(legacy);

        service.applyGranted(7L, USER_ID, "BANK_A", "BR_01", "ADVISOR");

        assertEquals(1, user.getBankBranchUsers().size());
        assertEquals(7L, legacy.getAssignmentId());
    }

    @Test
    void shouldRemoveARevokedAssignment() {
        final BranchUserEntity row = BranchUserEntity.builder()
                .user(user).bankCode("BANK_A").branchCode("BR_01").role("CUSTOMER").assignmentId(9L).build();
        user.getBankBranchUsers().add(row);
        when(branchUserRepository.findByAssignmentId(9L)).thenReturn(Optional.of(row));

        service.applyRevoked(9L);

        assertTrue(user.getBankBranchUsers().isEmpty());
    }

    @Test
    void shouldSkipAGrantForAnUnknownUser() {
        final UUID unknown = UUID.randomUUID();

        service.applyGranted(7L, unknown, "BANK_A", "BR_01", "ADVISOR");

        verify(userRepository, never()).save(any());
    }
}
