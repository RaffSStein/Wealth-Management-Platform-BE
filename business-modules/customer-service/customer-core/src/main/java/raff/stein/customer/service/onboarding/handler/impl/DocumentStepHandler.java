package raff.stein.customer.service.onboarding.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.entity.customer.CustomerOnboardingEntity;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStatus;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

import java.util.UUID;

@Component
@Slf4j
public class DocumentStepHandler extends BaseOnboardingStepHandler {

    @Override
    public OnboardingStep getHandledStep() {
        return OnboardingStep.DOCUMENTS;
    }

    @Override
    public String buildStepReason(OnboardingStepContext context) {
        final UUID fileId = (UUID) context.getMetadata("fileId");
        final Boolean isValid = (Boolean) context.getMetadata("isValid");
        return Boolean.TRUE.equals(isValid) ?
                String.format("File with ID: [%s] has been validated successfully.", fileId.toString()) :
                String.format("File with ID: [%s] has been rejected.", fileId.toString());
    }

    @Override
    public String buildStepStatus(OnboardingStepContext context) {
        final Boolean isValid = (Boolean) context.getMetadata("isValid");
        return Boolean.TRUE.equals(isValid) ? "DONE" : "REJECTED";
    }

    @Override
    public void updateCustomerOnboardingStatus(
            CustomerOnboardingEntity customerOnboarding,
            OnboardingStepContext context) {
        final UUID customerId = context.getCustomerId();
        final UUID fileId = (UUID) context.getMetadata("fileId");
        final Boolean isValid = (Boolean) context.getMetadata("isValid");

        if (Boolean.TRUE.equals(isValid)) {
            log.info("File with ID: [{}] is valid. Proceeding to next onboarding step for customer ID: [{}].", fileId, customerId);
            // Proceed to the next step in the onboarding process
            customerOnboarding.setOnboardingStatus(OnboardingStatus.IN_PROGRESS);
        } else {
            log.warn("File with ID: [{}] is not valid. Marking onboarding as failed for customer ID: [{}].", fileId, customerId);
            // Mark the onboarding as failed
            customerOnboarding.setOnboardingStatus(OnboardingStatus.FAILED);
        }
    }

    @Override
    public void updateCustomerOnboardingReason(CustomerOnboardingEntity customerOnboarding, OnboardingStepContext context) {
        final Boolean isValid = (Boolean) context.getMetadata("isValid");
        if (Boolean.TRUE.equals(isValid)) {
            customerOnboarding.setReason("Document validated successfully");
        } else {
            customerOnboarding.setReason("Document validation failed");
        }

    }
}
