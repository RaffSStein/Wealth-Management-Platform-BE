package raff.stein.customer.service.onboarding.handler.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.entity.customer.CustomerOnboardingEntity;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerGoalsStepHandler extends BaseOnboardingStepHandler {

    @Override
    public OnboardingStep getHandledStep() {
        return OnboardingStep.GOALS;
    }

    @Override
    public String buildStepReason(OnboardingStepContext context) {
        return "Goals step completed successfully ";
    }

    @Override
    public String buildStepStatus(OnboardingStepContext context) {
        return "DONE";
    }

    @Override
    public void updateCustomerOnboardingStatus(CustomerOnboardingEntity customerOnboarding, OnboardingStepContext context) {
        // do nothing, this step does not update the status of the onboarding entity
    }

    @Override
    public void updateCustomerOnboardingReason(CustomerOnboardingEntity customerOnboarding, OnboardingStepContext context) {
        customerOnboarding.setReason("Goals step completed successfully");

    }

}
