package raff.stein.customer.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.customer.exception.CustomerNotFoundException;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.service.access.CustomerAccessPolicy;
import raff.stein.customer.service.onboarding.OnboardingService;
import raff.stein.customer.service.update.visitor.CustomerVisitorDispatcher;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.conflict.ResourceConflictException;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerServiceTest {

    private static final UUID UNKNOWN_CUSTOMER_ID = UUID.randomUUID();
    private static final UUID CALLER_ID = UUID.randomUUID();

    private CustomerRepository customerRepository;
    private CustomerVisitorDispatcher customerVisitorDispatcher;
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        customerVisitorDispatcher = mock(CustomerVisitorDispatcher.class);
        customerService = new CustomerService(
                customerRepository,
                mock(OnboardingService.class),
                customerVisitorDispatcher,
                new CustomerAccessPolicy(customerRepository));
        when(customerRepository.findById(UNKNOWN_CUSTOMER_ID)).thenReturn(Optional.empty());
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

    private static Customer customerFor(UUID userId) {
        return Customer.builder()
                .userId(userId)
                .customerType("INDIVIDUAL")
                .customerStatus("ACTIVE")
                .build();
    }

    private void stubSave() {
        when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void shouldTakeUserIdFromTokenWhenCustomerInits() {
        signInAs("CUSTOMER");
        stubSave();

        final Customer created = customerService.initCustomer(customerFor(null));

        assertEquals(CALLER_ID, created.getUserId());
    }

    @Test
    void shouldAlwaysStartAsDraftWhateverTheBodySays() {
        for (final String role : List.of("CUSTOMER", "ADVISOR")) {
            signInAs(role);
            stubSave();
            final Customer input = customerFor(CALLER_ID);
            input.setCustomerStatus("ACTIVE");
            input.setOnboardingDate(LocalDate.of(2020, 1, 1));

            final Customer created = customerService.initCustomer(input);

            assertEquals("DRAFT", created.getCustomerStatus(), role);
            assertNull(created.getOnboardingDate(), role);
        }
    }

    @Test
    void shouldRefuseAnotherUsersRecordWhenCustomerInits() {
        signInAs("CUSTOMER");

        assertThrows(AccessDeniedException.class, () -> customerService.initCustomer(customerFor(UUID.randomUUID())));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldRefuseSecondRecordWhenCustomerInits() {
        signInAs("CUSTOMER");
        when(customerRepository.existsByUserId(CALLER_ID)).thenReturn(true);

        final ResourceConflictException ex = assertThrows(
                ResourceConflictException.class,
                () -> customerService.initCustomer(customerFor(CALLER_ID)));

        assertEquals(ErrorCode.CUSTOMER_ALREADY_EXISTS, ex.getErrorCode());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldLetAdvisorCreateSeveralRecordsForTheUserItSends() {
        signInAs("ADVISOR");
        stubSave();
        final UUID userId = UUID.randomUUID();
        when(customerRepository.existsByUserId(userId)).thenReturn(true);

        final Customer created = customerService.initCustomer(customerFor(userId));

        assertEquals(userId, created.getUserId());
    }

    @Test
    void shouldThrowCustomerNotFoundWhenGettingUnknownCustomer() {
        final CustomerNotFoundException ex = assertThrows(
                CustomerNotFoundException.class,
                () -> customerService.getCustomerById(UNKNOWN_CUSTOMER_ID));

        assertEquals(ErrorCode.OBJECT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldThrowCustomerNotFoundAndSkipVisitorsWhenUpdatingUnknownCustomer() {
        assertThrows(
                CustomerNotFoundException.class,
                () -> customerService.updateCustomer(UNKNOWN_CUSTOMER_ID, List.of()));

        verify(customerVisitorDispatcher, never()).dispatchAndVisit(any(), any());
    }
}
