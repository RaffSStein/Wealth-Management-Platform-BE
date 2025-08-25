package raff.stein.customer.service.onboarding;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;
import raff.stein.customer.service.onboarding.handler.OnboardingStepDispatcher;

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
}
