package raff.stein.customer.service.mifid.workflow.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.customer.event.producer.CustomerOnboardedEventPublisher;
import raff.stein.customer.model.bo.mifid.filling.CustomerRiskProfile;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.enumeration.CustomerStatus;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.repository.mifid.MifidFillingRepository;
import raff.stein.customer.repository.mifid.MifidQuestionnaireRepository;
import raff.stein.customer.service.mifid.command.impl.SubmitMifidCommand;
import raff.stein.customer.service.mifid.workflow.step.state.impl.SubmitMifidStateHandler;
import raff.stein.customer.service.onboarding.OnboardingService;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmitMifidWorkflowTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final Long FILLING_ID = 10L;

    private OnboardingService onboardingService;
    private CustomerOnboardedEventPublisher customerOnboardedEventPublisher;
    private CustomerRepository customerRepository;
    private MifidFillingRepository mifidFillingRepository;
    private SubmitMifidWorkflow submitMifidWorkflow;

    @BeforeEach
    void setUp() {
        final SubmitMifidCommand submitMifidCommand = mock(SubmitMifidCommand.class);
        final SubmitMifidStateHandler submitMifidStateHandler = mock(SubmitMifidStateHandler.class);
        onboardingService = mock(OnboardingService.class);
        customerOnboardedEventPublisher = mock(CustomerOnboardedEventPublisher.class);
        customerRepository = mock(CustomerRepository.class);
        mifidFillingRepository = mock(MifidFillingRepository.class);
        submitMifidWorkflow = new SubmitMifidWorkflow(
                submitMifidCommand,
                submitMifidStateHandler,
                mock(MifidQuestionnaireRepository.class),
                onboardingService,
                customerOnboardedEventPublisher,
                customerRepository,
                mifidFillingRepository);

        final MifidFillingEntity persistedFilling = MifidFillingEntity.builder()
                .id(FILLING_ID)
                .customerId(CUSTOMER_ID)
                .questionnaireId(1L)
                .status(MifidFillingStatus.COMPLETED)
                .responses(new HashSet<>())
                .build();
        when(submitMifidStateHandler.findTargetFilling(any(), eq(CUSTOMER_ID))).thenReturn(Optional.of(persistedFilling));
        when(submitMifidCommand.execute(eq(CUSTOMER_ID), any())).thenReturn(MifidFilling.builder()
                .fillingId(FILLING_ID)
                .status(MifidFillingStatus.SUBMITTED)
                .customerRiskProfile(CustomerRiskProfile.builder().profile("BALANCED").score(42).build())
                .build());
    }

    @Test
    void shouldDeprecatePreviousSubmittedFillingAndSkipOnboardingOnRenewal() {
        final CustomerEntity activeCustomer = customer(CustomerStatus.ACTIVE);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(activeCustomer));
        final MifidFillingEntity previousSubmitted = MifidFillingEntity.builder()
                .id(4L)
                .customerId(CUSTOMER_ID)
                .status(MifidFillingStatus.SUBMITTED)
                .build();
        when(mifidFillingRepository.findByCustomerIdAndStatusAndIdNot(CUSTOMER_ID, MifidFillingStatus.SUBMITTED, FILLING_ID))
                .thenReturn(List.of(previousSubmitted));

        submitMifidWorkflow.execute(CUSTOMER_ID, MifidFilling.builder().fillingId(FILLING_ID).build());

        assertEquals(MifidFillingStatus.DEPRECATED, previousSubmitted.getStatus());
        assertEquals(CustomerStatus.ACTIVE, activeCustomer.getCustomerStatus());
        verify(onboardingService, never()).proceedToStep(any(), any());
        verify(customerOnboardedEventPublisher, never()).publishCustomerOnboardedEvent(any(), any());
    }

    @Test
    void shouldCompleteOnboardingOnFirstSubmit() {
        final CustomerEntity draftCustomer = customer(CustomerStatus.DRAFT);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(draftCustomer));
        when(mifidFillingRepository.findByCustomerIdAndStatusAndIdNot(CUSTOMER_ID, MifidFillingStatus.SUBMITTED, FILLING_ID))
                .thenReturn(List.of());

        submitMifidWorkflow.execute(CUSTOMER_ID, MifidFilling.builder().fillingId(FILLING_ID).build());

        assertEquals(CustomerStatus.PENDING_VERIFICATION, draftCustomer.getCustomerStatus());
        verify(onboardingService).proceedToStep(eq(OnboardingStep.MIFID), any());
        verify(customerOnboardedEventPublisher).publishCustomerOnboardedEvent(any(), eq(CUSTOMER_ID));
    }

    private static CustomerEntity customer(CustomerStatus status) {
        return CustomerEntity.builder()
                .id(CUSTOMER_ID)
                .customerStatus(status)
                .build();
    }
}
