package raff.stein.customer.service.onboarding.handler.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.customer.exception.CustomerException;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MifidStepHandler extends BaseOnboardingStepHandler {

    @Override
    public OnboardingStep getHandledStep() {
        return OnboardingStep.MIFID;
    }

    @Override
    public String buildStepReason(OnboardingStepContext context) {
        final MifidFillingStatus mifidStatus = Optional.ofNullable((String) context.getMetadata().get("mifidStatus"))
                .map(MifidFillingStatus::valueOf)
                .orElseThrow(() -> new CustomerException("MIFID status is missing in the context metadata"));
        // save produces DRAFT or COMPLETED (all answered, not yet submitted); only submit produces SUBMITTED
        // no default branch: a new status must be handled here explicitly or the build fails
        return switch (mifidStatus) {
            case DRAFT -> "MIFID step initiated";
            case COMPLETED -> "MIFID questionnaire completed, awaiting submission";
            case SUBMITTED -> "MIFID step completed";
            case DEPRECATED -> throw new CustomerException("Unexpected MIFID status in the context metadata: [" + mifidStatus + "]");
        };
    }

    @Override
    public String buildStepStatus(OnboardingStepContext context) {
        return "DONE";
    }

    @Override
    public void updateCustomerOnboardingStatus(CustomerOnboardingEntity customerOnboarding, OnboardingStepContext context) {
        // do nothing, this step does not require any updates of the onboarding status
        // the MIFID status does not affect the onboarding status, it will remain IN_PROGRESS until the final step is completed
    }

    @Override
    public void updateCustomerOnboardingReason(CustomerOnboardingEntity customerOnboarding, OnboardingStepContext context) {
        customerOnboarding.setReason("MIFID step completed");
    }

}
