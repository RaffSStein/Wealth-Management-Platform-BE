package raff.stein.customer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.customer.exception.CustomerNotFoundException;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.service.onboarding.OnboardingService;
import raff.stein.customer.service.update.visitor.CustomerVisitorDispatcher;
import raff.stein.platformcore.exception.ErrorCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerServiceTest {

    private static final UUID UNKNOWN_CUSTOMER_ID = UUID.randomUUID();

    private CustomerRepository customerRepository;
    private CustomerVisitorDispatcher customerVisitorDispatcher;
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        customerVisitorDispatcher = mock(CustomerVisitorDispatcher.class);
        customerService = new CustomerService(customerRepository, mock(OnboardingService.class), customerVisitorDispatcher);
        when(customerRepository.findById(UNKNOWN_CUSTOMER_ID)).thenReturn(Optional.empty());
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
