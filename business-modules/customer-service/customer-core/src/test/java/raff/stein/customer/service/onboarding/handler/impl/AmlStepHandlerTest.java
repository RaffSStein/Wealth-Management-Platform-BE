package raff.stein.customer.service.onboarding.handler.impl;

import org.junit.jupiter.api.Test;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStatus;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;
import raff.stein.customer.repository.onboarding.CustomerOnboardingRepository;
import raff.stein.customer.repository.onboarding.CustomerOnboardingStepRepository;
import raff.stein.customer.service.aml.pipeline.AmlResult;
import raff.stein.customer.service.aml.pipeline.step.AmlStepResult;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class AmlStepHandlerTest {

    private final AmlStepHandler amlStepHandler = new AmlStepHandler(
            mock(CustomerOnboardingRepository.class),
            mock(CustomerOnboardingStepRepository.class));

    @Test
    void shouldHandleAmlStep() {
        assertEquals(OnboardingStep.AML, amlStepHandler.getHandledStep());
    }

    @Test
    void shouldKeepOnboardingInProgressWhenAmlPassed() {
        final OnboardingStepContext context = contextWith(AmlStepResult.StepStatus.PASSED);
        final CustomerOnboardingEntity onboarding = onboardingInProgress();

        amlStepHandler.updateCustomerOnboardingStatus(onboarding, context);
        amlStepHandler.updateCustomerOnboardingReason(onboarding, context);

        assertEquals("AML check completed with status: [PASSED]", amlStepHandler.buildStepReason(context));
        assertEquals("DONE", amlStepHandler.buildStepStatus(context));
        assertEquals(OnboardingStatus.IN_PROGRESS, onboarding.getOnboardingStatus());
        assertEquals("AML check passed successfully", onboarding.getReason());
    }

    @Test
    void shouldFailOnboardingWhenAmlFailed() {
        final OnboardingStepContext context = contextWith(AmlStepResult.StepStatus.FAILED);
        final CustomerOnboardingEntity onboarding = onboardingInProgress();

        amlStepHandler.updateCustomerOnboardingStatus(onboarding, context);
        amlStepHandler.updateCustomerOnboardingReason(onboarding, context);

        assertEquals("AML check completed with status: [FAILED]", amlStepHandler.buildStepReason(context));
        assertEquals("REJECTED", amlStepHandler.buildStepStatus(context));
        assertEquals(OnboardingStatus.FAILED, onboarding.getOnboardingStatus());
        assertEquals("AML check failed", onboarding.getReason());
    }

    @Test
    void shouldFailOnboardingWhenAmlResultIsMissing() {
        final OnboardingStepContext context = OnboardingStepContext.builder()
                .customerId(UUID.randomUUID())
                .metadata(Map.of())
                .build();
        final CustomerOnboardingEntity onboarding = onboardingInProgress();

        amlStepHandler.updateCustomerOnboardingStatus(onboarding, context);
        amlStepHandler.updateCustomerOnboardingReason(onboarding, context);

        assertEquals("Error: AML result data is missing.", amlStepHandler.buildStepReason(context));
        assertEquals("REJECTED", amlStepHandler.buildStepStatus(context));
        assertEquals(OnboardingStatus.FAILED, onboarding.getOnboardingStatus());
        assertEquals("AML result data is missing", onboarding.getReason());
    }

    private CustomerOnboardingEntity onboardingInProgress() {
        return CustomerOnboardingEntity.builder().onboardingStatus(OnboardingStatus.IN_PROGRESS).build();
    }

    private OnboardingStepContext contextWith(AmlStepResult.StepStatus overallStatus) {
        return OnboardingStepContext.builder()
                .customerId(UUID.randomUUID())
                .metadata(Map.of("AmlResult", AmlResult.builder().overallStatus(overallStatus).build()))
                .build();
    }
}
