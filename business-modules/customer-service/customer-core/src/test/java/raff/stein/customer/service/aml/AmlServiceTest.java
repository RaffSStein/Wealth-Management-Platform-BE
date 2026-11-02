package raff.stein.customer.service.aml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import raff.stein.customer.event.producer.CustomerCreatedEventPublisher;
import raff.stein.customer.event.producer.CustomerRejectedEventPublisher;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.entity.aml.AmlVerificationEntity;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.repository.aml.AmlVerificationRepository;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.service.aml.pipeline.AmlPipelineExecutor;
import raff.stein.customer.service.aml.pipeline.AmlResult;
import raff.stein.customer.service.aml.pipeline.step.AmlStepResult;
import raff.stein.customer.service.onboarding.OnboardingService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AmlServiceTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    private AmlPipelineExecutor amlPipelineExecutor;
    private AmlVerificationRepository amlVerificationRepository;
    private OnboardingService onboardingService;
    private CustomerCreatedEventPublisher customerCreatedEventPublisher;
    private CustomerRejectedEventPublisher customerRejectedEventPublisher;
    private CustomerRepository customerRepository;
    private PlatformTransactionManager transactionManager;
    private AmlService amlService;

    @BeforeEach
    void setUp() {
        amlPipelineExecutor = mock(AmlPipelineExecutor.class);
        amlVerificationRepository = mock(AmlVerificationRepository.class);
        onboardingService = mock(OnboardingService.class);
        customerCreatedEventPublisher = mock(CustomerCreatedEventPublisher.class);
        customerRejectedEventPublisher = mock(CustomerRejectedEventPublisher.class);
        customerRepository = mock(CustomerRepository.class);
        transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        amlService = new AmlService(
                amlPipelineExecutor,
                amlVerificationRepository,
                onboardingService,
                customerCreatedEventPublisher,
                customerRejectedEventPublisher,
                customerRepository,
                new TransactionTemplate(transactionManager));
    }

    @Test
    void shouldSaveVerificationAgainstManagedCustomerAndPublishCreatedWhenPassed() {
        // the managed reference returned by the repository, as opposed to a copy mapped from the event payload
        final CustomerEntity managedCustomer = CustomerEntity.builder().id(CUSTOMER_ID).build();
        when(customerRepository.getReferenceById(CUSTOMER_ID)).thenReturn(managedCustomer);
        when(amlPipelineExecutor.execute(any())).thenReturn(amlResult(AmlStepResult.StepStatus.PASSED));

        amlService.triggerAmlCheck(customerFromEvent());

        final ArgumentCaptor<AmlVerificationEntity> savedVerification = ArgumentCaptor.forClass(AmlVerificationEntity.class);
        verify(amlVerificationRepository).save(savedVerification.capture());
        assertSame(managedCustomer, savedVerification.getValue().getCustomer());
        verify(onboardingService).proceedToStep(eq(OnboardingStep.AML), any());
        verify(customerCreatedEventPublisher).publishCustomerCreatedEvent(CUSTOMER_ID);
        verify(customerRejectedEventPublisher, never()).publishCustomerRejectedEvent(any());
    }

    @Test
    void shouldPublishRejectedWhenAmlFails() {
        when(customerRepository.getReferenceById(CUSTOMER_ID)).thenReturn(CustomerEntity.builder().id(CUSTOMER_ID).build());
        when(amlPipelineExecutor.execute(any())).thenReturn(amlResult(AmlStepResult.StepStatus.FAILED));

        amlService.triggerAmlCheck(customerFromEvent());

        verify(customerRejectedEventPublisher).publishCustomerRejectedEvent(CUSTOMER_ID);
        verify(customerCreatedEventPublisher, never()).publishCustomerCreatedEvent(any());
    }

    @Test
    void shouldStoreVerificationStepAndEventInOneTransaction() {
        when(customerRepository.getReferenceById(CUSTOMER_ID)).thenReturn(CustomerEntity.builder().id(CUSTOMER_ID).build());
        when(amlPipelineExecutor.execute(any())).thenReturn(amlResult(AmlStepResult.StepStatus.PASSED));

        amlService.triggerAmlCheck(customerFromEvent());

        // the outbox row is written by the publisher call, so it must sit between begin and commit
        final InOrder inOrder = inOrder(transactionManager, amlVerificationRepository, onboardingService,
                customerCreatedEventPublisher);
        inOrder.verify(transactionManager).getTransaction(any());
        inOrder.verify(amlVerificationRepository).save(any(AmlVerificationEntity.class));
        inOrder.verify(onboardingService).proceedToStep(eq(OnboardingStep.AML), any());
        inOrder.verify(customerCreatedEventPublisher).publishCustomerCreatedEvent(CUSTOMER_ID);
        inOrder.verify(transactionManager).commit(any());
    }

    @Test
    void shouldRollBackEverythingWhenTheEventCannotBeStored() {
        when(customerRepository.getReferenceById(CUSTOMER_ID)).thenReturn(CustomerEntity.builder().id(CUSTOMER_ID).build());
        when(amlPipelineExecutor.execute(any())).thenReturn(amlResult(AmlStepResult.StepStatus.PASSED));
        doThrow(new IllegalStateException("outbox write failed"))
                .when(customerCreatedEventPublisher).publishCustomerCreatedEvent(CUSTOMER_ID);

        assertThrows(IllegalStateException.class, () -> amlService.triggerAmlCheck(customerFromEvent()));

        // the verification and the onboarding step are undone with the event, so a redelivery starts clean
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void shouldRunTheAmlPipelineOutsideTheTransaction() {
        when(customerRepository.getReferenceById(CUSTOMER_ID)).thenReturn(CustomerEntity.builder().id(CUSTOMER_ID).build());
        when(amlPipelineExecutor.execute(any())).thenAnswer(call -> {
            // external providers: no database transaction may be open while they answer
            verify(transactionManager, never()).getTransaction(any());
            return amlResult(AmlStepResult.StepStatus.PASSED);
        });

        amlService.triggerAmlCheck(customerFromEvent());

        verify(transactionManager).getTransaction(any());
    }

    private Customer customerFromEvent() {
        return Customer.builder().id(CUSTOMER_ID).country("IT").build();
    }

    // Instant timestamps, as produced by AmlPipelineExecutor: converting them used to throw
    private AmlResult amlResult(AmlStepResult.StepStatus status) {
        final Instant now = Instant.now();
        return AmlResult.builder()
                .amlCaseId(UUID.randomUUID())
                .jurisdiction("eu")
                .executedSteps(List.of())
                .overallStatus(status)
                .startedAt(now)
                .finishedAt(now)
                .build();
    }
}
