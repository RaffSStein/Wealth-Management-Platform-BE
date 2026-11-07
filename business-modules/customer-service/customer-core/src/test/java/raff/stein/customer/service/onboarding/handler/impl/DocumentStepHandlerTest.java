package raff.stein.customer.service.onboarding.handler.impl;

import org.junit.jupiter.api.Test;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStatus;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;
import raff.stein.customer.repository.onboarding.CustomerOnboardingRepository;
import raff.stein.customer.repository.onboarding.CustomerOnboardingStepRepository;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class DocumentStepHandlerTest {

    private static final UUID FILE_ID = UUID.fromString("0b6f3c1e-6a43-4c8e-9a0e-1d2f3a4b5c6d");

    private final DocumentStepHandler documentStepHandler = new DocumentStepHandler(
            mock(CustomerOnboardingRepository.class),
            mock(CustomerOnboardingStepRepository.class));

    @Test
    void shouldHandleDocumentsStep() {
        assertEquals(OnboardingStep.DOCUMENTS, documentStepHandler.getHandledStep());
    }

    @Test
    void shouldBuildReasonAndStatusForValidFile() {
        final OnboardingStepContext context = contextWith(true);

        assertEquals("File with ID: [" + FILE_ID + "] has been validated successfully.",
                documentStepHandler.buildStepReason(context));
        assertEquals("DONE", documentStepHandler.buildStepStatus(context));
    }

    @Test
    void shouldBuildReasonAndStatusForRejectedFile() {
        final OnboardingStepContext context = contextWith(false);

        assertEquals("File with ID: [" + FILE_ID + "] has been rejected.", documentStepHandler.buildStepReason(context));
        assertEquals("REJECTED", documentStepHandler.buildStepStatus(context));
    }

    @Test
    void shouldKeepOnboardingInProgressAndSetReasonForValidFile() {
        final CustomerOnboardingEntity onboarding = onboardingInProgress();
        final OnboardingStepContext context = contextWith(true);

        documentStepHandler.updateCustomerOnboardingStatus(onboarding, context);
        documentStepHandler.updateCustomerOnboardingReason(onboarding, context);

        assertEquals(OnboardingStatus.IN_PROGRESS, onboarding.getOnboardingStatus());
        assertEquals("Document validated successfully", onboarding.getReason());
    }

    @Test
    void shouldFailOnboardingAndSetReasonForRejectedFile() {
        final CustomerOnboardingEntity onboarding = onboardingInProgress();
        final OnboardingStepContext context = contextWith(false);

        documentStepHandler.updateCustomerOnboardingStatus(onboarding, context);
        documentStepHandler.updateCustomerOnboardingReason(onboarding, context);

        assertEquals(OnboardingStatus.FAILED, onboarding.getOnboardingStatus());
        assertEquals("Document validation failed", onboarding.getReason());
    }

    private CustomerOnboardingEntity onboardingInProgress() {
        return CustomerOnboardingEntity.builder().onboardingStatus(OnboardingStatus.IN_PROGRESS).build();
    }

    private OnboardingStepContext contextWith(boolean isValid) {
        return OnboardingStepContext.builder()
                .customerId(UUID.randomUUID())
                .metadata(Map.of("fileId", FILE_ID, "isValid", isValid))
                .build();
    }
}
