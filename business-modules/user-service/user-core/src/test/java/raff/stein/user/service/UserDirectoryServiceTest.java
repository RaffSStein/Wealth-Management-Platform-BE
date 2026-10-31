package raff.stein.user.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.user.model.entity.BranchUserEntity;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.model.user.User;
import raff.stein.user.model.user.UserSummary;
import raff.stein.user.model.user.UserSummaryPage;
import raff.stein.user.repository.UserRepository;
import raff.stein.user.service.visibility.UserVisibilityPolicy;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDirectoryServiceTest {

    private UserRepository userRepository;
    private UserDirectoryService service;
    private UserEntity unlinkedCustomer;
    private UserEntity advisorBankB;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new UserDirectoryService(userRepository, new UserVisibilityPolicy());
        unlinkedCustomer = user("new@customer.test");
        advisorBankB = user("advisor@bank-b.test", assignment("BANK_B", "BR_09", "ADVISOR"));
        when(userRepository.findByEmailIgnoreCase("new@customer.test")).thenReturn(Optional.of(unlinkedCustomer));
        when(userRepository.findByEmailIgnoreCase("advisor@bank-b.test")).thenReturn(Optional.of(advisorBankB));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    void bankAdminShouldFindAnUnlinkedUser() {
        givenCaller("BANK_ADMIN");

        final UserSummary summary = service.lookupByEmail("new@customer.test");

        assertEquals(unlinkedCustomer.getId(), summary.getId());
        assertTrue(summary.getRoleAssignments().isEmpty());
    }

    @Test
    void advisorShouldNotFindAnUnlinkedUser() {
        givenCaller("ADVISOR");

        assertThrows(GenericObjectNotFoundException.class, () -> service.lookupByEmail("new@customer.test"));
    }

    @Test
    void outOfScopeShouldLookLikeNotFound() {
        givenCaller("BANK_ADMIN");

        assertThrows(GenericObjectNotFoundException.class, () -> service.lookupByEmail("advisor@bank-b.test"));
        assertThrows(GenericObjectNotFoundException.class, () -> service.lookupByEmail("nobody@nowhere.test"));
    }

    @Test
    void shouldRejectABlankEmail() {
        givenCaller("BANK_ADMIN");

        assertThrows(RequestValidationException.class, () -> service.lookupByEmail(" "));
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchShouldBeScopedPagedAndSortedByEmail() {
        givenCaller("BANK_ADMIN");
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(unlinkedCustomer), invocation.getArgument(1), 41));

        final UserSummaryPage page = service.search("customer", false, 2, 20);

        final ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(any(Specification.class), pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(Sort.by("email"), pageable.getValue().getSort());
        assertEquals(41, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
        assertEquals(1, page.getContent().size());
    }

    @Test
    void searchShouldValidateItsInputAndBeAdminOnly() {
        givenCaller("BANK_ADMIN");
        assertThrows(RequestValidationException.class, () -> service.search("ab", null, 0, 20));
        assertThrows(RequestValidationException.class, () -> service.search("abc", null, 0, 51));
        assertThrows(RequestValidationException.class, () -> service.search("abc", null, -1, 20));

        givenCaller("ADVISOR");
        assertThrows(AccessDeniedException.class, () -> service.search("abc", null, 0, 20));
    }

    @Test
    void getUserShouldHideUsersAndAssignmentsOutOfScope() {
        final BranchUserEntity bankA = assignment("BANK_A", "BR_01", "CUSTOMER");
        final UserEntity twoBanks = user("two@banks.test", bankA, assignment("BANK_B", "BR_09", "CUSTOMER"));
        when(userRepository.findById(twoBanks.getId())).thenReturn(Optional.of(twoBanks));
        when(userRepository.findById(advisorBankB.getId())).thenReturn(Optional.of(advisorBankB));
        givenCaller("BANK_ADMIN");

        final User visible = service.getVisibleUser(twoBanks.getId());

        assertEquals(1, visible.getBranchRoles().size());
        assertEquals("BANK_A", visible.getBranchRoles().getFirst().getBankCode());
        assertThrows(GenericObjectNotFoundException.class, () -> service.getVisibleUser(advisorBankB.getId()));
    }

    private static void givenCaller(String role) {
        SecurityContextHolder.setContext(WMPContext.builder()
                .userId(UUID.randomUUID().toString()).email("caller@bank-a.test").roles(Set.of(role))
                .tenantId("BANK_A").branchCode("BR_01").build());
    }

    private static UserEntity user(String email, BranchUserEntity... assignments) {
        final UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email(email)
                .firstName("Test").lastName("User").bankBranchUsers(new HashSet<>(Set.of(assignments))).build();
        for (BranchUserEntity assignment : assignments) {
            assignment.setUser(user);
        }
        return user;
    }

    private static BranchUserEntity assignment(String bankCode, String branchCode, String role) {
        return BranchUserEntity.builder().bankCode(bankCode).branchCode(branchCode).role(role).build();
    }
}
