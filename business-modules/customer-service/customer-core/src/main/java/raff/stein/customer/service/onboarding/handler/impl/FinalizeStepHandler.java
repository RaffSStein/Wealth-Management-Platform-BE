package raff.stein.customer.service.onboarding.handler.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinalizeStepHandler extends BaseOnboardingStepHandler {

    @Override
    public String buildStepReason(OnboardingStepContext context) {
        return "";
    }

    @Override
    public String buildStepStatus(OnboardingStepContext context) {
        return "";
    }

    @Override
    public void updateCustomerOnboardingStatus(CustomerOnboardingEntity customerOnboarding, OnboardingStepContext context) {

    }

    @Override
    public void updateCustomerOnboardingReason(CustomerOnboardingEntity customerOnboarding, OnboardingStepContext context) {

    }

    @Override
    public OnboardingStep getHandledStep() {
        return OnboardingStep.FINALIZE;
    }
}
