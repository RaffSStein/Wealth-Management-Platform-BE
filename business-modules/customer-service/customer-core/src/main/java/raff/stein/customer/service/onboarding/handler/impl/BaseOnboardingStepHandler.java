package raff.stein.customer.service.onboarding.handler.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import raff.stein.customer.exception.CustomerException;
import raff.stein.customer.model.entity.customer.CustomerOnboardingEntity;
import raff.stein.customer.model.entity.customer.CustomerOnboardingStepEntity;
import raff.stein.customer.repository.onboarding.CustomerOnboardingRepository;
import raff.stein.customer.repository.onboarding.CustomerOnboardingStepRepository;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;
import raff.stein.customer.service.onboarding.handler.OnboardingStepHandler;

import java.util.Optional;
import java.util.UUID;

/**
 * Base class for onboarding step handlers.
 * Provides a default implementation of the handle method.
 */
@Slf4j
public abstract class BaseOnboardingStepHandler implements OnboardingStepHandler {

    @Autowired
    protected CustomerOnboardingRepository customerOnboardingRepository;
    @Autowired
    protected CustomerOnboardingStepRepository customerOnboardingStepRepository;

    /**
     * This method should return the step reason built based on the context.
     */
    public abstract String buildStepReason(OnboardingStepContext context);

    /**
     * This method should return the step status built based on the context.
     */
    public abstract String buildStepStatus(OnboardingStepContext context);

    /**
     * This method should update some fields of the customer onboarding entity
     * based on the context.
     */
    public abstract void updateCustomerOnboardingEntity(
            CustomerOnboardingEntity customerOnboarding,
            OnboardingStepContext context);

    /**
     * Handles the onboarding step for the given context.
     * This method performs the following actions:
     * 1. Checks and retrieves the last valid customer onboarding instance.<br>
     * 2. Updates or inserts the new step for this onboarding instance. <br>
     * 3. Updates the onboarding status. <br>
     *
     * @param context the onboarding step context containing customer ID and other relevant data
     * @throws CustomerException if the customer ID is null or if no valid onboarding instance is found
     */
    @Override
    public void handle(@NonNull OnboardingStepContext context) {
        // 1. check and get the last valid customer onboarding instance
        UUID customerId = context.getCustomerId();
        if (customerId == null) {
            throw new CustomerException("Customer ID must not be null");
        }

        Optional<CustomerOnboardingEntity> customerOnboardingOptional =
                customerOnboardingRepository.findByCustomerIdAndIsValidTrue(customerId);
        if (customerOnboardingOptional.isEmpty()) {
            log.warn("No valid customer onboarding found for customer ID: [{}]. Skipping onboarding step handling.", customerId);
            return;
        }
        CustomerOnboardingEntity customerOnboarding = customerOnboardingOptional.get();

        // 2. update or insert the new step for this onboarding instance
        Optional<CustomerOnboardingStepEntity> existingStepOptional =
                customerOnboardingStepRepository.findByCustomerOnboardingAndStep(
                        customerOnboarding,
                        getHandledStep());
        // perform logics here for the 2 fields
        final String reason = buildStepReason(context);
        final String status = buildStepStatus(context);

        if(existingStepOptional.isPresent()) {
            // Step already exists, update it
            CustomerOnboardingStepEntity existingStep = existingStepOptional.get();
            existingStep.setReason(reason);
            existingStep.setStatus(status);
            customerOnboardingStepRepository.save(existingStep);
            log.info("Updated onboarding step [{}] for customer ID: [{}] and onboarding ID: [{}].",
                    getHandledStep(),
                    customerId,
                    customerOnboarding.getId());
        } else {
            // Step does not exist, create a new one
            CustomerOnboardingStepEntity newStep = CustomerOnboardingStepEntity.builder()
                    .customerOnboardingId(customerOnboarding.getId())
                    .step(getHandledStep())
                    .stepOrder(getHandledStep().getOrder())
                    .status(status)
                    .reason(reason)
                    .build();
            customerOnboardingStepRepository.save(newStep);
            log.info("Created new onboarding step [{}] for customer ID: [{}] and onboarding ID: [{}].",
                    getHandledStep(),
                    customerId,
                    customerOnboarding.getId());
        }

        // 3. update the onboarding status
        updateCustomerOnboardingEntity(customerOnboarding, context);
    }
}
