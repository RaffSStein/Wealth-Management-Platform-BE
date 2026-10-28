package raff.stein.customer.service.onboarding.handler.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.enumeration.CustomerStatus;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStatus;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FinalizeStepHandlerTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    private CustomerEntity customer;
    private CustomerOnboardingEntity onboarding;
    private FinalizeStepHandler finalizeStepHandler;

    @BeforeEach
    void setUp() {
        final CustomerRepository customerRepository = mock(CustomerRepository.class);
        customer = CustomerEntity.builder().id(CUSTOMER_ID).customerStatus(CustomerStatus.PENDING_VERIFICATION).build();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        onboarding = CustomerOnboardingEntity.builder().customerId(CUSTOMER_ID).onboardingStatus(OnboardingStatus.IN_PROGRESS).build();
        finalizeStepHandler = new FinalizeStepHandler(customerRepository);
    }

    @Test
    void shouldActivateCustomerWhenAmlPassed() {
        final OnboardingStepContext context = contextWith(Map.of(FinalizeStepHandler.AML_PASSED_METADATA_KEY, true));

        finalizeStepHandler.updateCustomerOnboardingStatus(onboarding, context);

        assertEquals(CustomerStatus.ACTIVE, customer.getCustomerStatus());
        assertEquals(OnboardingStatus.DONE, onboarding.getOnboardingStatus());
        assertEquals("DONE", finalizeStepHandler.buildStepStatus(context));
    }

    @Test
    void shouldRejectCustomerWhenAmlFailed() {
        final OnboardingStepContext context = contextWith(Map.of(FinalizeStepHandler.AML_PASSED_METADATA_KEY, false));

        finalizeStepHandler.updateCustomerOnboardingStatus(onboarding, context);

        assertEquals(CustomerStatus.REJECTED, customer.getCustomerStatus());
        assertEquals(OnboardingStatus.FAILED, onboarding.getOnboardingStatus());
        assertEquals("REJECTED", finalizeStepHandler.buildStepStatus(context));
    }

    @Test
    void shouldNotActivateCustomerWhenAmlOutcomeIsMissing() {
        final OnboardingStepContext context = contextWith(null);

        finalizeStepHandler.updateCustomerOnboardingStatus(onboarding, context);

        assertEquals(CustomerStatus.REJECTED, customer.getCustomerStatus());
    }

    private OnboardingStepContext contextWith(Map<String, Object> metadata) {
        return OnboardingStepContext.builder().customerId(CUSTOMER_ID).metadata(metadata).build();
    }
}
