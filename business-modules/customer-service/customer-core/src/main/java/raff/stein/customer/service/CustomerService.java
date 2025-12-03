package raff.stein.customer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.customer.mapper.CustomerToCustomerEntityMapper;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.service.onboarding.OnboardingService;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;
import raff.stein.customer.service.update.visitor.CustomerVisitorDispatcher;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    private final OnboardingService onboardingService;
    private final CustomerVisitorDispatcher customerVisitorDispatcher;

    private static final CustomerToCustomerEntityMapper customerToCustomerEntityMapper = CustomerToCustomerEntityMapper.MAPPER;

    @Transactional
    public Customer initCustomer(Customer customer) {
        log.debug("Initializing customer: [{}]", customer);
        // Save the customer entity to the database
        CustomerEntity savedCustomerEntity = customerRepository.save(customerToCustomerEntityMapper.toCustomerEntity(customer));
        // Initiate the onboarding process for the saved customer
        onboardingService.proceedToStep(
                OnboardingStep.INIT,
                OnboardingStepContext.builder()
                        .customerId(savedCustomerEntity.getId())
                        .build());
        return customerToCustomerEntityMapper.toCustomer(savedCustomerEntity);
    }

    @Transactional
    public Customer updateCustomer(UUID customerId, Object customerAttributesToUpdate) {
        log.debug("Updating fields [{}] for customer ID: [{}]", customerAttributesToUpdate, customerId);
        // Lookup the customer entity by ID
        CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        // Convert the customer entity to a business object
        Customer customer = customerToCustomerEntityMapper.toCustomer(customerEntity);
        // Update customer attributes via a visitor pattern
        return customerVisitorDispatcher.dispatchAndVisit(customer, customerAttributesToUpdate);
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(UUID customerId) {
        log.debug("Retrieving customer by ID: [{}]", customerId);
        CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        return customerToCustomerEntityMapper.toCustomer(customerEntity);
    }

    @Transactional(readOnly = true)
    public List<Customer> getCustomersByUserId(UUID userId) {
        log.debug("Retrieving customers for userId: [{}]", userId);
        List<CustomerEntity> customerEntities = customerRepository.findByUserId(userId);
        if (customerEntities.isEmpty()) {
            throw new IllegalArgumentException("No customers found for user ID: " + userId);
        }
        return customerEntities
                .stream()
                .map(customerToCustomerEntityMapper::toCustomer)
                .toList();
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
