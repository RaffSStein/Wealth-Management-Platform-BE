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
public class CustomerFinancialsStepHandler extends BaseOnboardingStepHandler {

    @Override
    public OnboardingStep getHandledStep() {
        return OnboardingStep.FINANCIALS;
    }

    @Override
    public String buildStepReason(OnboardingStepContext context) {
        return "Financials step completed successfully";
    }

    @Override
    public String buildStepStatus(OnboardingStepContext context) {
        return "DONE";
    }

    @Override
    public void updatedCustomerOnboardingEntity(CustomerOnboardingEntity customerOnboarding, OnboardingStepContext context) {
        // do nothing, this step does not require any updates to the onboarding entity
    }

}
