package raff.stein.customer.service.onboarding.handler.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStatus;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingStepEntity;
import raff.stein.customer.repository.onboarding.CustomerOnboardingRepository;
import raff.stein.customer.repository.onboarding.CustomerOnboardingStepRepository;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;
import raff.stein.customer.service.onboarding.handler.OnboardingStepHandler;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InitStepHandler implements OnboardingStepHandler {

    private final CustomerOnboardingRepository customerOnboardingRepository;
    private final CustomerOnboardingStepRepository customerOnboardingStepRepository;

    @Override
    public OnboardingStep getHandledStep() {
        return OnboardingStep.INIT;
    }

    @Override
    public void handle(@NonNull OnboardingStepContext context) {
        log.info("Starting onboarding process for customer with ID [{}].", context.getCustomerId());
        // check if the customer already has an onboarding process instance active
        Optional<CustomerOnboardingEntity> existingOnboardingOptional =
                customerOnboardingRepository.findByCustomerIdAndIsValidTrue(context.getCustomerId());
        existingOnboardingOptional.ifPresent(onb -> {
            log.warn("Customer with ID [{}] already has an active onboarding process. Disabling the old one", context.getCustomerId());
            // if the customer already has an active onboarding process, disable it
            onb.setValid(false);
        });

        final CustomerOnboardingEntity onboardingEntity = CustomerOnboardingEntity.builder()
                .customerId(context.getCustomerId())
                .onboardingStatus(OnboardingStatus.IN_PROGRESS)
                .reason("Onboarding process initiated")
                .isValid(true)
                .build();
        CustomerOnboardingEntity savedCustomerOnboardingEntity = customerOnboardingRepository.save(onboardingEntity);
        // create the first step of the onboarding process
        final CustomerOnboardingStepEntity firstStep = CustomerOnboardingStepEntity.builder()
                .customerOnboardingId(savedCustomerOnboardingEntity.getId())
                .step(OnboardingStep.INIT)
                .stepOrder(OnboardingStep.INIT.getOrder())
                .status("DONE")
                .reason("Onboarding process initiated")
                .build();
        customerOnboardingStepRepository.save(firstStep);
    }
}
