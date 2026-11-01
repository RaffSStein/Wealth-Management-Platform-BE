package raff.stein.customer.service.link;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.repository.customer.CustomerRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerUserLinkServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "client@bank.test";

    private CustomerRepository customerRepository;
    private CustomerUserLinkService service;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        service = new CustomerUserLinkService(customerRepository);
    }

    private static CustomerEntity record(UUID userId, String branchCode) {
        return CustomerEntity.builder().id(UUID.randomUUID()).userId(userId).email(EMAIL).branchCode(branchCode).build();
    }

    @Test
    void shouldLinkTheWaitingRecordWhateverTheEmailCase() {
        final CustomerEntity waiting = record(null, "BR_01");
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(waiting));

        service.linkOnRoleGranted(USER_ID, "Client@Bank.TEST", "BR_01");

        assertEquals(USER_ID, waiting.getUserId());
        verify(customerRepository).save(waiting);
    }

    @Test
    void shouldDoNothingWithoutAWaitingRecord() {
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        service.linkOnRoleGranted(USER_ID, EMAIL, "BR_01");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldNotLinkARecordOfAnotherBranch() {
        final CustomerEntity otherBranch = record(null, "BR_02");
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(otherBranch));

        service.linkOnRoleGranted(USER_ID, EMAIL, "BR_01");

        assertNull(otherBranch.getUserId());
    }

    @Test
    void shouldChangeNothingOnAReplay() {
        final CustomerEntity linked = record(USER_ID, "BR_01");
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(linked));

        service.linkOnRoleGranted(USER_ID, EMAIL, "BR_01");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldNotTakeARecordLinkedToSomeoneElse() {
        final UUID someoneElse = UUID.randomUUID();
        final CustomerEntity linked = record(someoneElse, "BR_01");
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(linked));

        service.linkOnRoleGranted(USER_ID, EMAIL, "BR_01");

        assertEquals(someoneElse, linked.getUserId());
    }

    @Test
    void shouldLeaveTheRecordUnlinkedWhenTheUserAlreadyHasOne() {
        final CustomerEntity waiting = record(null, "BR_01");
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(waiting));
        when(customerRepository.existsByUserId(USER_ID)).thenReturn(true);

        service.linkOnRoleGranted(USER_ID, EMAIL, "BR_01");

        assertNull(waiting.getUserId());
    }

    @Test
    void shouldUnlinkWhenTheRoleIsRevokedAndKeepTheEmail() {
        final CustomerEntity linked = record(USER_ID, "BR_01");
        when(customerRepository.findByUserIdAndBranchCode(USER_ID, "BR_01")).thenReturn(List.of(linked));

        service.unlinkOnRoleRevoked(USER_ID, "BR_01");

        assertNull(linked.getUserId());
        assertEquals(EMAIL, linked.getEmail());
    }

    @Test
    void shouldIgnoreARevokeWithNothingLinked() {
        when(customerRepository.findByUserIdAndBranchCode(USER_ID, "BR_01")).thenReturn(List.of());

        service.unlinkOnRoleRevoked(USER_ID, "BR_01");

        verify(customerRepository, never()).save(any());
    }
}
