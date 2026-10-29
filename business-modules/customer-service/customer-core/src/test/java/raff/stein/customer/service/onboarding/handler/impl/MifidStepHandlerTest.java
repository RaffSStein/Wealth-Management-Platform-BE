package raff.stein.customer.service.onboarding.handler.impl;

import org.junit.jupiter.api.Test;
import raff.stein.customer.exception.CustomerException;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MifidStepHandlerTest {

    private final MifidStepHandler mifidStepHandler = new MifidStepHandler();

    @Test
    void shouldBuildInitiatedReasonForDraftFilling() {
        assertEquals("MIFID step initiated", mifidStepHandler.buildStepReason(contextWith(MifidFillingStatus.DRAFT.name())));
    }

    @Test
    void shouldBuildAwaitingSubmissionReasonForCompletedFilling() {
        assertEquals("MIFID questionnaire completed, awaiting submission",
                mifidStepHandler.buildStepReason(contextWith(MifidFillingStatus.COMPLETED.name())));
    }

    @Test
    void shouldBuildCompletedReasonForSubmittedFilling() {
        assertEquals("MIFID step completed", mifidStepHandler.buildStepReason(contextWith(MifidFillingStatus.SUBMITTED.name())));
    }

    @Test
    void shouldRejectMissingMifidStatus() {
        final OnboardingStepContext context = OnboardingStepContext.builder()
                .customerId(UUID.randomUUID())
                .metadata(Map.of())
                .build();
        assertThrows(CustomerException.class, () -> mifidStepHandler.buildStepReason(context));
    }

    @Test
    void shouldRejectUnexpectedMifidStatus() {
        final OnboardingStepContext context = contextWith(MifidFillingStatus.DEPRECATED.name());
        assertThrows(CustomerException.class, () -> mifidStepHandler.buildStepReason(context));
    }

    private OnboardingStepContext contextWith(String mifidStatus) {
        return OnboardingStepContext.builder()
                .customerId(UUID.randomUUID())
                .metadata(Map.of("mifidStatus", mifidStatus))
                .build();
    }
}
