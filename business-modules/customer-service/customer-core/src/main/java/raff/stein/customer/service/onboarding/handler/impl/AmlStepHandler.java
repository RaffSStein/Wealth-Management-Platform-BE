package raff.stein.customer.service.onboarding.handler.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.entity.customer.CustomerOnboardingEntity;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStatus;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.service.aml.pipeline.AmlResult;
import raff.stein.customer.service.aml.pipeline.step.AmlStepResult;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AmlStepHandler extends BaseOnboardingStepHandler {

    @Override
    public OnboardingStep getHandledStep() {
        return OnboardingStep.AML;
    }

    @Override
    public String buildStepReason(OnboardingStepContext context) {
        final AmlResult amlResult = (AmlResult) context.getMetadata("AmlResult");
        if (amlResult != null) {
            return String.format("AML check completed with status: [%s]", amlResult.overallStatus());
        }
        return "Error: AML result data is missing.";
    }

    @Override
    public String buildStepStatus(OnboardingStepContext context) {
        final AmlResult amlResult = (AmlResult) context.getMetadata("AmlResult");
        if (amlResult != null) {
            return amlResult.overallStatus().equals(AmlStepResult.StepStatus.PASSED) ? "DONE" : "REJECTED";
        }
        return "REJECTED";
    }

    @Override
    public void updateCustomerOnboardingEntity(CustomerOnboardingEntity customerOnboarding, OnboardingStepContext context) {
        final UUID customerId = context.getCustomerId();
        final AmlResult amlResult = (AmlResult) context.getMetadata("AmlResult");
        if (amlResult != null) {
            if (amlResult.overallStatus().equals(AmlStepResult.StepStatus.PASSED)) {
                log.info("AML check passed for customer ID: [{}]. Proceeding to next onboarding step.", customerId);
                // Proceed to the next step in the onboarding process
                customerOnboarding.setOnboardingStatus(OnboardingStatus.IN_PROGRESS);
                customerOnboarding.setReason("AML check passed successfully");
            } else {
                log.warn("AML check failed for customer ID: [{}]. Marking onboarding as failed.", customerId);
                // Mark the onboarding as failed
                customerOnboarding.setOnboardingStatus(OnboardingStatus.AML_REJECTED);
                customerOnboarding.setReason("AML check failed");
            }
        } else {
            log.error("AML result data is missing for customer ID: [{}]. Marking onboarding as failed.", customerId);
            // Mark the onboarding as failed due to missing AML result
            customerOnboarding.setOnboardingStatus(OnboardingStatus.AML_REJECTED);
            customerOnboarding.setReason("AML result data is missing");
        }


    }
}
