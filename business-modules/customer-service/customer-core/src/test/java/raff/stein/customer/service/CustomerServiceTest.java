package raff.stein.customer.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openapitools.model.CreateUserEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import raff.stein.customer.exception.CustomerNotFoundException;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.enumeration.CustomerStatus;
import raff.stein.customer.model.entity.customer.enumeration.CustomerType;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.service.access.CustomerAccessPolicy;
import raff.stein.customer.service.onboarding.OnboardingService;
import raff.stein.customer.service.update.visitor.CustomerVisitorDispatcher;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;
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
    private static final String CALLER_EMAIL = "caller@bank.test";
    private static final String CLIENT_EMAIL = "client@bank.test";

    private CustomerRepository customerRepository;
    private CustomerVisitorDispatcher customerVisitorDispatcher;
    private ApplicationEventPublisher eventPublisher;
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        customerVisitorDispatcher = mock(CustomerVisitorDispatcher.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        customerService = new CustomerService(
                customerRepository,
                mock(OnboardingService.class),
                customerVisitorDispatcher,
                new CustomerAccessPolicy(customerRepository),
                eventPublisher);
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
                .branchCode("BR_01")
                .email(CALLER_EMAIL)
                .build());
    }

    private static Customer customerFor(UUID userId) {
        return Customer.builder()
                .userId(userId)
                .email(CLIENT_EMAIL)
                .firstName("Ada")
                .lastName("Lovelace")
                .phoneNumber("+390000000")
                .customerType("INDIVIDUAL")
                .customerStatus("ACTIVE")
                .build();
    }

    private void stubSave() {
        when(customerRepository.saveAndFlush(any(CustomerEntity.class))).thenAnswer(call -> call.getArgument(0));
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
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRefuseSecondRecordWhenCustomerInits() {
        signInAs("CUSTOMER");
        when(customerRepository.existsByUserId(CALLER_ID)).thenReturn(true);

        final ResourceConflictException ex = assertThrows(
                ResourceConflictException.class,
                () -> customerService.initCustomer(customerFor(CALLER_ID)));

        assertEquals(ErrorCode.CUSTOMER_ALREADY_EXISTS, ex.getErrorCode());
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldStartAdvisorRecordUnlinkedInTheBranchOfTheToken() {
        signInAs("ADVISOR");
        stubSave();
        final Customer input = customerFor(CALLER_ID);
        input.setBranchCode("BR_99");

        final Customer created = customerService.initCustomer(input);

        assertNull(created.getUserId());
        assertEquals("BR_01", created.getBranchCode());
    }

    @Test
    void shouldAnswerConflictWhenTheDatabaseRefusesASecondRecord() {
        signInAs("CUSTOMER");
        when(customerRepository.saveAndFlush(any(CustomerEntity.class))).thenThrow(new DataIntegrityViolationException(
                "duplicate", new RuntimeException("violates " + CustomerEntity.TENANT_USER_CONSTRAINT)));

        final ResourceConflictException ex = assertThrows(
                ResourceConflictException.class,
                () -> customerService.initCustomer(customerFor(CALLER_ID)));

        assertEquals(ErrorCode.CUSTOMER_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void shouldKeepOtherDatabaseErrorsAsTheyAre() {
        signInAs("CUSTOMER");
        when(customerRepository.saveAndFlush(any(CustomerEntity.class))).thenThrow(new DataIntegrityViolationException(
                "other", new RuntimeException("null value in column tax_id")));

        assertThrows(DataIntegrityViolationException.class, () -> customerService.initCustomer(customerFor(CALLER_ID)));
    }

    @Test
    void shouldLimitAdvisorDisambiguationToItsBranch() {
        signInAs("ADVISOR");
        final UUID userId = UUID.randomUUID();
        when(customerRepository.findByUserIdAndBranchCode(userId, "BR_01")).thenReturn(List.of());

        customerService.getCustomersByUserId(userId);

        verify(customerRepository).findByUserIdAndBranchCode(userId, "BR_01");
        verify(customerRepository, never()).findByUserId(any());
    }

    @Test
    void shouldAskForTheClientLoginWhenAdvisorInits() {
        signInAs("ADVISOR");
        stubSave();

        customerService.initCustomer(customerFor(null));

        final ArgumentCaptor<CreateUserEvent> event = ArgumentCaptor.forClass(CreateUserEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(CLIENT_EMAIL, event.getValue().getUser().getEmail());
        assertEquals("BANK_A", event.getValue().getBankCode());
        assertEquals("BR_01", event.getValue().getBranchCode());
        assertEquals(CALLER_EMAIL, event.getValue().getRequestedBy());
    }

    @Test
    void shouldRefuseAdvisorWithoutWhatTheLoginNeeds() {
        signInAs("ADVISOR");
        final List<java.util.function.Consumer<Customer>> gaps = List.of(
                customer -> customer.setFirstName(null),
                customer -> customer.setLastName(" "),
                customer -> customer.setPhoneNumber(null));
        for (final java.util.function.Consumer<Customer> gap : gaps) {
            final Customer input = customerFor(null);
            gap.accept(input);

            assertThrows(RequestValidationException.class, () -> customerService.initCustomer(input));
        }
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldNotRequireThoseFieldsWhenCustomerInits() {
        signInAs("CUSTOMER");
        stubSave();
        final Customer input = customerFor(null);
        input.setPhoneNumber(null);

        assertEquals(CALLER_EMAIL, customerService.initCustomer(input).getEmail());
    }

    @Test
    void shouldNotAskForALoginWhenCustomerInits() {
        signInAs("CUSTOMER");
        stubSave();

        final Customer created = customerService.initCustomer(customerFor(null));

        assertEquals(CALLER_EMAIL, created.getEmail());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void shouldRefuseAdvisorWhenTheBankAlreadyHasThatEmail() {
        signInAs("ADVISOR");
        when(customerRepository.findByEmail(CLIENT_EMAIL))
                .thenReturn(Optional.of(customerEntity(UUID.randomUUID(), null, "BR_02")));

        final ResourceConflictException ex = assertThrows(
                ResourceConflictException.class,
                () -> customerService.initCustomer(customerFor(null)));

        assertEquals(ErrorCode.CUSTOMER_ALREADY_EXISTS, ex.getErrorCode());
        verify(customerRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void shouldHandCustomerTheRecordWaitingForTheRoleEvent() {
        signInAs("CUSTOMER");
        stubSave();
        final UUID waitingId = UUID.randomUUID();
        when(customerRepository.findByEmail(CALLER_EMAIL))
                .thenReturn(Optional.of(customerEntity(waitingId, null, "BR_01")));

        final Customer result = customerService.initCustomer(customerFor(null));

        assertEquals(waitingId, result.getId());
        assertEquals(CALLER_ID, result.getUserId());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void shouldNotHandCustomerARecordOfAnotherBranch() {
        signInAs("CUSTOMER");
        when(customerRepository.findByEmail(CALLER_EMAIL))
                .thenReturn(Optional.of(customerEntity(UUID.randomUUID(), null, "BR_02")));

        assertThrows(ResourceConflictException.class, () -> customerService.initCustomer(customerFor(null)));
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRefuseUnknownStatusInTheList() {
        signInAs("ADVISOR");

        assertThrows(RequestValidationException.class,
                () -> customerService.findCustomers(null, List.of("BOGUS"), PageRequest.of(0, 10)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSortTheListByNameUnlessAsked() {
        signInAs("ADVISOR");
        when(customerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        customerService.findCustomers(null, null, PageRequest.of(1, 20));
        customerService.findCustomers(null, null, PageRequest.of(0, 10, Sort.by("taxId")));

        final ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(customerRepository, times(2)).findAll(any(Specification.class), pageable.capture());
        assertEquals(PageRequest.of(1, 20, Sort.by("lastName", "firstName", "companyName")), pageable.getAllValues().getFirst());
        assertEquals(Sort.by("taxId"), pageable.getAllValues().getLast().getSort());
    }

    private static CustomerEntity customerEntity(UUID customerId, UUID userId, String branchCode) {
        return CustomerEntity.builder()
                .id(customerId)
                .userId(userId)
                .branchCode(branchCode)
                .customerType(CustomerType.INDIVIDUAL)
                .customerStatus(CustomerStatus.DRAFT)
                .build();
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
