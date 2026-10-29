package raff.stein.customer.service.onboarding.handler.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.customer.exception.CustomerNotFoundException;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.enumeration.CustomerStatus;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStatus;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

/**
 * Closes the onboarding once the AML outcome is known (CustomerCreatedEvent / CustomerRejectedEvent):
 * AML passed -> customer ACTIVE, onboarding DONE; AML failed -> customer REJECTED, onboarding FAILED.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FinalizeStepHandler extends BaseOnboardingStepHandler {

    public static final String AML_PASSED_METADATA_KEY = "amlPassed";

    private final CustomerRepository customerRepository;

    @Override
    public String buildStepReason(OnboardingStepContext context) {
        return isAmlPassed(context)
                ? "Onboarding completed: customer activated"
                : "Onboarding closed: customer rejected after AML check";
    }

    @Override
    public String buildStepStatus(OnboardingStepContext context) {
        return isAmlPassed(context) ? "DONE" : "REJECTED";
    }

    @Override
    public void updateCustomerOnboardingStatus(CustomerOnboardingEntity customerOnboarding, OnboardingStepContext context) {
        final boolean amlPassed = isAmlPassed(context);
        customerOnboarding.setOnboardingStatus(amlPassed ? OnboardingStatus.DONE : OnboardingStatus.FAILED);
        // the customer status follows the onboarding outcome; this runs only while the onboarding is IN_PROGRESS,
        // so a replayed event can't flip an already closed onboarding
        final CustomerEntity customerEntity = customerRepository.findById(context.getCustomerId())
                .orElseThrow(CustomerNotFoundException.with(context.getCustomerId()));
        customerEntity.setCustomerStatus(amlPassed ? CustomerStatus.ACTIVE : CustomerStatus.REJECTED);
        log.info("Finalized onboarding for customer ID: [{}] with customer status: [{}]",
                context.getCustomerId(), customerEntity.getCustomerStatus());
    }

    @Override
    public void updateCustomerOnboardingReason(CustomerOnboardingEntity customerOnboarding, OnboardingStepContext context) {
        customerOnboarding.setReason(buildStepReason(context));
    }

    @Override
    public OnboardingStep getHandledStep() {
        return OnboardingStep.FINALIZE;
    }

    // a missing outcome is treated as not passed: never activate a customer without an explicit AML pass
    private boolean isAmlPassed(OnboardingStepContext context) {
        return Boolean.TRUE.equals(context.getMetadata(AML_PASSED_METADATA_KEY));
    }
}
