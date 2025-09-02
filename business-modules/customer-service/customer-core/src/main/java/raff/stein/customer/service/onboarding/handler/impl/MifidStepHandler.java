package raff.stein.customer.service.onboarding.handler.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.customer.exception.CustomerException;
import raff.stein.customer.model.entity.customer.CustomerOnboardingEntity;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

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
        final String mifidStatus = (String) context.getMetadata().get("mifidStatus");
        if (mifidStatus != null) {
            if (mifidStatus.equals(MifidFillingStatus.DRAFT.name())) {
                return "MIFID step initiated";
            } else if (mifidStatus.equals(MifidFillingStatus.SUBMITTED.name())) {
                return "MIFID step completed";
            }
        }
        throw new CustomerException("MIFID status is missing in the context metadata");
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
