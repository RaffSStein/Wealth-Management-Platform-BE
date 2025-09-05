package raff.stein.customer.service.onboarding;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.customer.model.bo.onboarding.CustomerOnboarding;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;
import raff.stein.customer.service.onboarding.handler.OnboardingStepDispatcher;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final OnboardingStepDispatcher onboardingStepDispatcher;

    @Transactional
    public void proceedToStep(OnboardingStep onboardingStep, OnboardingStepContext onboardingStepContext) {
        log.info("Proceeding to onboarding step [{}] for customer ID: [{}].", onboardingStep, onboardingStepContext.getCustomerId());
        // Dispatch the step to the appropriate handler
        onboardingStepDispatcher.dispatch(onboardingStep, onboardingStepContext);
    }

    @Transactional(readOnly = true)
    public CustomerOnboarding getActiveOnboarding(UUID customerId) {
        // Implementation to retrieve active onboarding
        return null;
    }

    @Transactional(readOnly = true)
    public CustomerOnboarding getOnboardingById(UUID customerId, Long onboardingId) {
        // Implementation to retrieve onboarding by ID
        return null;
    }

    @Transactional(readOnly = true)
    public List<CustomerOnboarding> getOnboardingInstances(UUID customerId) {
        // Implementation to retrieve all onboarding instances for a customer
        return null;
    }
}
