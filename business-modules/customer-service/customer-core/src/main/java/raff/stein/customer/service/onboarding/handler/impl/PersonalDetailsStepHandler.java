package raff.stein.customer.service.onboarding.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;
import raff.stein.customer.repository.onboarding.CustomerOnboardingRepository;
import raff.stein.customer.repository.onboarding.CustomerOnboardingStepRepository;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

@Component
@Slf4j
public class PersonalDetailsStepHandler extends BaseOnboardingStepHandler {

    public PersonalDetailsStepHandler(
            CustomerOnboardingRepository customerOnboardingRepository,
            CustomerOnboardingStepRepository customerOnboardingStepRepository) {
        super(customerOnboardingRepository, customerOnboardingStepRepository);
    }

    @Override
    public OnboardingStep getHandledStep() {
        return OnboardingStep.PERSONAL_DETAILS;
    }

    @Override
    public String buildStepReason(OnboardingStepContext context) {
        return "Personal details step completed successfully";
    }

    @Override
    public String buildStepStatus(OnboardingStepContext context) {
        return "DONE";
    }

    @Override
    public void updateCustomerOnboardingStatus(
            CustomerOnboardingEntity customerOnboarding,
            OnboardingStepContext context) {
        // do nothing, this step does not update the status of the onboarding entity
    }

    @Override
    public void updateCustomerOnboardingReason(
            CustomerOnboardingEntity customerOnboarding,
            OnboardingStepContext context) {
        customerOnboarding.setReason("Personal details step completed successfully");
    }
}
