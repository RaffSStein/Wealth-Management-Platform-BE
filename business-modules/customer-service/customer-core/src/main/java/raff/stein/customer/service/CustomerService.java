package raff.stein.customer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.CreateUserEvent;
import org.openapitools.model.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.customer.exception.CustomerNotFoundException;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.bo.customer.CustomerSummary;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.enumeration.CustomerStatus;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.customer.mapper.CustomerToCustomerEntityMapper;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.repository.customer.CustomerSpecifications;
import raff.stein.customer.service.access.CustomerAccessPolicy;
import raff.stein.customer.service.onboarding.OnboardingService;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;
import raff.stein.customer.service.update.visitor.CustomerVisitorDispatcher;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;
import raff.stein.platformcore.exception.types.conflict.ResourceConflictException;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    private final OnboardingService onboardingService;
    private final CustomerVisitorDispatcher customerVisitorDispatcher;
    private final CustomerAccessPolicy customerAccessPolicy;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final Sort DEFAULT_LIST_SORT = Sort.by("lastName", "firstName", "companyName");

    private static final CustomerToCustomerEntityMapper customerToCustomerEntityMapper = CustomerToCustomerEntityMapper.MAPPER;

    @Transactional
    public Customer initCustomer(Customer customer) {
        log.debug("Initializing customer: [{}]", customer);
        final UUID ownerId = customerAccessPolicy.resolveInitUserId(customer.getUserId());
        final String email = customerAccessPolicy.resolveInitEmail(customer.getEmail());
        final String branchCode = customerAccessPolicy.resolveInitBranchCode();
        final boolean selfService = customerAccessPolicy.isLimitedToOneRecord();
        if (!selfService) {
            requireClientLoginFields(customer);
        }
        final Optional<CustomerEntity> sameEmail = customerRepository.findByEmail(email);
        if (selfService) {
            // the advisor's record may be waiting for the role event: the token proves the same person, so don't make them wait
            if (sameEmail.filter(entity -> entity.getUserId() == null && branchCode.equals(entity.getBranchCode())).isPresent()
                    && !customerRepository.existsByUserId(ownerId)) {
                final CustomerEntity waiting = sameEmail.get();
                waiting.setUserId(ownerId);
                return customerToCustomerEntityMapper.toCustomer(saveWithOneRecordPerBank(waiting, ownerId));
            }
            // one record per bank for a customer; the tenant filter scopes this check to the bank of the token
            if (customerRepository.existsByUserId(ownerId)) {
                throw ResourceConflictException.of(ErrorCode.CUSTOMER_ALREADY_EXISTS, ownerId);
            }
        }
        if (sameEmail.isPresent()) {
            throw ResourceConflictException.of(ErrorCode.CUSTOMER_ALREADY_EXISTS, email);
        }
        customer.setUserId(ownerId);
        customer.setEmail(email);
        customer.setBranchCode(branchCode);
        // the status only moves through the onboarding steps: a client must not be able to start as ACTIVE and skip them
        customer.setCustomerStatus(CustomerStatus.DRAFT.name());
        customer.setOnboardingDate(null);
        // Save the customer entity to the database
        CustomerEntity savedCustomerEntity = saveWithOneRecordPerBank(
                customerToCustomerEntityMapper.toCustomerEntity(customer), email);
        // Initiate the onboarding process for the saved customer
        onboardingService.proceedToStep(
                OnboardingStep.INIT,
                OnboardingStepContext.builder()
                        .customerId(savedCustomerEntity.getId())
                        .build());
        if (!selfService) {
            requestClientLogin(customer);
        }
        return customerToCustomerEntityMapper.toCustomer(savedCustomerEntity);
    }

    // the login needs them too: refused here, because a failure later in user-service would leave no login and no email
    private static void requireClientLoginFields(Customer customer) {
        requireText("firstName", customer.getFirstName());
        requireText("lastName", customer.getLastName());
        requireText("phoneNumber", customer.getPhoneNumber());
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            RequestValidationException.of(ErrorCode.REQUEST_VALIDATION_ERROR, field, String.valueOf(value));
        }
    }

    // stored in the outbox with the record, so a rolled-back record never creates a login or an email
    private void requestClientLogin(Customer customer) {
        final WMPContext caller = SecurityContextHolder.getContextOrThrow();
        applicationEventPublisher.publishEvent(new CreateUserEvent()
                .user(new User()
                        .email(customer.getEmail())
                        .firstName(customer.getFirstName())
                        .lastName(customer.getLastName())
                        .phoneNumber(customer.getPhoneNumber()))
                .bankCode(caller.getTenantId())
                .branchCode(customer.getBranchCode())
                .requestedBy(caller.getEmail()));
    }

    /**
     * The customers the caller can reach: the branch for an advisor, otherwise the bank (the tenant filter). Deleted
     * records are never listed. Sorted by name unless the request says otherwise.
     */
    @Transactional(readOnly = true)
    public Page<CustomerSummary> findCustomers(String query, List<String> statuses, Pageable pageable) {
        final Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_LIST_SORT);
        return customerRepository
                .findAll(
                        CustomerSpecifications.listed(customerAccessPolicy.branchScope(), query, toStatuses(statuses)),
                        sorted)
                .map(customerToCustomerEntityMapper::toCustomerSummary);
    }

    private static List<CustomerStatus> toStatuses(List<String> statuses) {
        if (statuses == null) {
            return List.of();
        }
        return statuses.stream().map(status -> {
            try {
                return CustomerStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new RequestValidationException(
                        ErrorCode.REQUEST_VALIDATION_ERROR,
                        String.format(ErrorCode.REQUEST_VALIDATION_ERROR.getMessageTemplate(), "status", status));
            }
        }).toList();
    }

    @Transactional
    public Customer updateCustomer(UUID customerId, Object customerAttributesToUpdate) {
        log.debug("Updating fields [{}] for customer ID: [{}]", customerAttributesToUpdate, customerId);
        // Lookup the customer entity by ID
        CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(CustomerNotFoundException.with(customerId));
        // Convert the customer entity to a business object
        Customer customer = customerToCustomerEntityMapper.toCustomer(customerEntity);
        // Update customer attributes via a visitor pattern
        return customerVisitorDispatcher.dispatchAndVisit(customer, customerAttributesToUpdate);
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(UUID customerId) {
        log.debug("Retrieving customer by ID: [{}]", customerId);
        CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(CustomerNotFoundException.with(customerId));
        return customerToCustomerEntityMapper.toCustomer(customerEntity);
    }

    @Transactional(readOnly = true)
    public List<Customer> getCustomersByUserId(UUID userId) {
        log.debug("Retrieving customers for userId: [{}]", userId);
        List<CustomerEntity> customerEntities = customerAccessPolicy.branchScope()
                .map(branchCode -> customerRepository.findByUserIdAndBranchCode(userId, branchCode))
                .orElseGet(() -> customerRepository.findByUserId(userId));
        return customerEntities
                .stream()
                .map(customerToCustomerEntityMapper::toCustomer)
                .toList();
    }

    // the check before saving can't stop two simultaneous requests: the database constraint decides those
    private CustomerEntity saveWithOneRecordPerBank(CustomerEntity customerEntity, Object client) {
        try {
            return customerRepository.saveAndFlush(customerEntity);
        } catch (DataIntegrityViolationException e) {
            final String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && (cause.contains(CustomerEntity.TENANT_USER_CONSTRAINT)
                    || cause.contains(CustomerEntity.TENANT_EMAIL_CONSTRAINT))) {
                throw ResourceConflictException.of(ErrorCode.CUSTOMER_ALREADY_EXISTS, client);
            }
            throw e;
        }
    }

    public void proceedToOnboardingStep(
            UUID customerId,
            OnboardingStep onboardingStep) {
        log.debug("Proceeding to onboarding step [{}] for customer ID: [{}]", onboardingStep, customerId);
        // Dispatch the step to the appropriate handler
        onboardingService.proceedToStep(
                onboardingStep,
                OnboardingStepContext.builder()
                        .customerId(customerId)
                        .build());
    }
}
