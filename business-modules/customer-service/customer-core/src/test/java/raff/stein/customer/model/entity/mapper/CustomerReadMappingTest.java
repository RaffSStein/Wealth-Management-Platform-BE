package raff.stein.customer.model.entity.mapper;

import org.junit.jupiter.api.Test;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.bo.onboarding.CustomerOnboarding;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.enumeration.CustomerStatus;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStatus;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.customer.mapper.CustomerToCustomerEntityMapper;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingStepEntity;
import raff.stein.customer.model.entity.onboarding.mapper.CustomerOnboardingToCustomerOnboardingEntityMapper;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomerReadMappingTest {

    @Test
    void shouldMapOnboardingStepsSortedByOrder() {
        final CustomerOnboardingEntity onboarding = CustomerOnboardingEntity.builder()
                .customerId(UUID.randomUUID())
                .onboardingStatus(OnboardingStatus.IN_PROGRESS)
                .onboardingSteps(Set.of(
                        step(OnboardingStep.MIFID),
                        step(OnboardingStep.INIT),
                        step(OnboardingStep.GOALS)))
                .build();

        final CustomerOnboarding result = CustomerOnboardingToCustomerOnboardingEntityMapper.MAPPER.toCustomerOnboarding(onboarding);

        assertEquals(3, result.getSteps().size());
        assertEquals(OnboardingStep.INIT, result.getSteps().get(0).getStep());
        assertEquals(OnboardingStep.GOALS, result.getSteps().get(1).getStep());
        assertEquals(OnboardingStep.MIFID, result.getSteps().get(2).getStep());
    }

    @Test
    void shouldMapPersistedRiskProfileOnCustomer() {
        final CustomerEntity customer = CustomerEntity.builder()
                .customerStatus(CustomerStatus.ACTIVE)
                .riskProfile("Moderately Aggressive Profile")
                .riskScore(116)
                .build();

        final Customer result = CustomerToCustomerEntityMapper.MAPPER.toCustomer(customer);

        assertEquals("Moderately Aggressive Profile", result.getRiskProfile().getProfile());
        assertEquals(116, result.getRiskProfile().getScore());
    }

    @Test
    void shouldLeaveRiskProfileEmptyBeforeSubmit() {
        final CustomerEntity customer = CustomerEntity.builder().customerStatus(CustomerStatus.DRAFT).build();

        assertNull(CustomerToCustomerEntityMapper.MAPPER.toCustomer(customer).getRiskProfile());
    }

    private CustomerOnboardingStepEntity step(OnboardingStep onboardingStep) {
        return CustomerOnboardingStepEntity.builder()
                .step(onboardingStep)
                .stepOrder(onboardingStep.getOrder())
                .status("DONE")
                .build();
    }
}
