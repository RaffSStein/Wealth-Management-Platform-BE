package raff.stein.customer.service.onboarding.handler.impl;

import org.junit.jupiter.api.Test;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStatus;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;
import raff.stein.customer.repository.onboarding.CustomerOnboardingRepository;
import raff.stein.customer.repository.onboarding.CustomerOnboardingStepRepository;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Personal details, financials and goals steps always complete and never change the onboarding status.
 */
class DataCollectionStepHandlersTest {

    private final CustomerOnboardingRepository onboardingRepository = mock(CustomerOnboardingRepository.class);
    private final CustomerOnboardingStepRepository stepRepository = mock(CustomerOnboardingStepRepository.class);

    @Test
    void shouldCompletePersonalDetailsStep() {
        assertStepCompleted(
                new PersonalDetailsStepHandler(onboardingRepository, stepRepository),
                OnboardingStep.PERSONAL_DETAILS,
                "Personal details step completed successfully",
                "Personal details step completed successfully");
    }

    @Test
    void shouldCompleteFinancialsStep() {
        assertStepCompleted(
                new CustomerFinancialsStepHandler(onboardingRepository, stepRepository),
                OnboardingStep.FINANCIALS,
                "Financials step completed successfully",
                "Financials step completed successfully");
    }

    @Test
    void shouldCompleteGoalsStep() {
        // The step reason has a trailing space, the onboarding reason doesn't: pinned as they are today
        assertStepCompleted(
                new CustomerGoalsStepHandler(onboardingRepository, stepRepository),
                OnboardingStep.GOALS,
                "Goals step completed successfully ",
                "Goals step completed successfully");
    }

    private void assertStepCompleted(
            BaseOnboardingStepHandler handler,
            OnboardingStep expectedStep,
            String expectedStepReason,
            String expectedOnboardingReason) {
        final OnboardingStepContext context = OnboardingStepContext.builder().customerId(UUID.randomUUID()).build();
        final CustomerOnboardingEntity onboarding =
                CustomerOnboardingEntity.builder().onboardingStatus(OnboardingStatus.IN_PROGRESS).build();

        handler.updateCustomerOnboardingStatus(onboarding, context);
        handler.updateCustomerOnboardingReason(onboarding, context);

        assertEquals(expectedStep, handler.getHandledStep());
        assertEquals(expectedStepReason, handler.buildStepReason(context));
        assertEquals("DONE", handler.buildStepStatus(context));
        assertEquals(OnboardingStatus.IN_PROGRESS, onboarding.getOnboardingStatus());
        assertEquals(expectedOnboardingReason, onboarding.getReason());
    }
}
