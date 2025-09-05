package raff.stein.customer.model.bo.onboarding;

import lombok.Builder;
import lombok.Data;
import org.openapitools.model.OnboardingStatus;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CustomerOnboarding {

    private Long id;
    private UUID customerId;
    private OnboardingStatus onboardingStatus;
    private String reason;
    private Boolean valid;
    private List<CustomerOnboardingStep> steps;
}

