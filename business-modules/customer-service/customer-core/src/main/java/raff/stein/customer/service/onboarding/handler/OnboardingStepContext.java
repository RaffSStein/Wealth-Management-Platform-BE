package raff.stein.customer.service.onboarding.handler;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class OnboardingStepContext {

    private Long onboardingId;
    private UUID customerId;
    // Additional metadata that can be used by the step handler
    private Map<String, Object> metadata;

    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
}
