package raff.stein.customer.service.onboarding.handler;

import lombok.NonNull;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;

public interface OnboardingStepHandler {

    OnboardingStep getHandledStep();

    void handle(@NonNull OnboardingStepContext context);

}
