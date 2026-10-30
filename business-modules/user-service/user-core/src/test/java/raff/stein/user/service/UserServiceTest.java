package raff.stein.user.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.user.event.producer.UserCreatedEventPublisher;
import raff.stein.user.model.BranchRole;
import raff.stein.user.model.entity.BranchUserEntity;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.model.user.User;
import raff.stein.user.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private static final String EMAIL = "advisor@bank.test";

    private UserService userService;

    @BeforeEach
    void setUp() {
        final UserRepository userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository, mock(UserCreatedEventPublisher.class));
        final UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .firstName("Ada")
                .lastName("Rossi")
                .phoneNumber("000")
                .bankBranchUsers(new HashSet<>(Set.of(
                        membership(1L, "BANK_A", "BR_01", "ADVISOR"),
                        membership(2L, "BANK_B", "BR_09", "CUSTOMER"))))
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    void shouldMarkOnlyTheMembershipOfTheCurrentToken() {
        SecurityContextHolder.setContext(WMPContext.builder()
                .email(EMAIL)
                .tenantId("BANK_B")
                .branchCode("BR_09")
                .build());

        final User user = userService.getCurrentUser();

        final List<BranchRole> active = user.getBranchRoles().stream().filter(BranchRole::isActive).toList();
        assertEquals(1, active.size());
        assertEquals("CUSTOMER", active.getFirst().getRole());
    }

    @Test
    void shouldMarkNoMembershipForTokenWithoutTenant() {
        SecurityContextHolder.setContext(WMPContext.builder().email(EMAIL).build());

        final User user = userService.getCurrentUser();

        assertEquals(2, user.getBranchRoles().size());
        assertTrue(user.getBranchRoles().stream().noneMatch(BranchRole::isActive));
    }

    private static BranchUserEntity membership(Long id, String bankCode, String branchCode, String role) {
        return BranchUserEntity.builder()
                .id(id)
                .bankCode(bankCode)
                .branchCode(branchCode)
                .role(role)
                .build();
    }
}
