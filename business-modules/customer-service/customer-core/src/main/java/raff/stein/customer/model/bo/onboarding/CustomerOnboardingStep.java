package raff.stein.customer.model.bo.onboarding;

import lombok.Builder;
import lombok.Data;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;

@Data
@Builder
public class CustomerOnboardingStep {

    private Long id;
    private OnboardingStep step;
    private String status;
    private String reason;
    private int stepOrder;
}
