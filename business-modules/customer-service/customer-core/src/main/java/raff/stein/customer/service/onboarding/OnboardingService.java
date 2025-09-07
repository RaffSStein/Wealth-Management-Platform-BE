package raff.stein.customer.service.onboarding;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.customer.exception.CustomerException;
import raff.stein.customer.model.bo.onboarding.CustomerOnboarding;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;
import raff.stein.customer.model.entity.onboarding.mapper.CustomerOnboardingToCustomerOnboardingEntityMapper;
import raff.stein.customer.repository.onboarding.CustomerOnboardingRepository;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;
import raff.stein.customer.service.onboarding.handler.OnboardingStepDispatcher;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final OnboardingStepDispatcher onboardingStepDispatcher;
    private final CustomerOnboardingRepository customerOnboardingRepository;

    private static final CustomerOnboardingToCustomerOnboardingEntityMapper customerOnboardingToCustomerOnboardingEntityMapper =
            CustomerOnboardingToCustomerOnboardingEntityMapper.MAPPER;

    @Transactional
    public void proceedToStep(OnboardingStep onboardingStep, OnboardingStepContext onboardingStepContext) {
        log.info("Proceeding to onboarding step [{}] for customer ID: [{}].", onboardingStep, onboardingStepContext.getCustomerId());
        // Dispatch the step to the appropriate handler
        onboardingStepDispatcher.dispatch(onboardingStep, onboardingStepContext);
    }

    @Transactional(readOnly = true)
    public CustomerOnboarding getActiveOnboarding(UUID customerId) {
        CustomerOnboardingEntity activeOnboarding = customerOnboardingRepository
                .findByCustomerIdAndIsValidTrue(customerId)
                .orElseThrow(() -> {
            log.warn("No active onboarding found for customer ID: [{}].", customerId);
            return new CustomerException("No active onboarding found for customer with id " + customerId);
        });
        return customerOnboardingToCustomerOnboardingEntityMapper.toCustomerOnboarding(activeOnboarding);
    }

    @Transactional(readOnly = true)
    public CustomerOnboarding getOnboardingById(UUID customerId, Long onboardingId) {
        CustomerOnboardingEntity onboarding = customerOnboardingRepository
                .findByIdAndCustomerId(onboardingId, customerId)
                .orElseThrow(() -> {
                    log.warn("No onboarding found with ID: [{}] for customer ID: [{}].", onboardingId, customerId);
                    return new CustomerException("No onboarding found with id " + onboardingId + " for customer with id " + customerId);
                });
        return customerOnboardingToCustomerOnboardingEntityMapper.toCustomerOnboarding(onboarding);
    }

    @Transactional(readOnly = true)
    public List<CustomerOnboarding> getOnboardingInstances(UUID customerId) {
        List<CustomerOnboardingEntity> onboardingEntities = customerOnboardingRepository
                .findAllByCustomerId(customerId);
        return onboardingEntities
                .stream()
                .map(customerOnboardingToCustomerOnboardingEntityMapper::toCustomerOnboarding)
                .toList();
    }
}
