package raff.stein.customer.controller;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.OnboardingApi;
import org.openapitools.model.CustomerOnboardingDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import raff.stein.customer.controller.mapper.onboarding.CustomerOnboardingDTOToCustomerOnboardingMapper;
import raff.stein.customer.model.bo.onboarding.CustomerOnboarding;
import raff.stein.customer.controller.access.CustomerAccess;
import raff.stein.customer.controller.access.CustomerAccessLevel;
import raff.stein.customer.service.onboarding.OnboardingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OnboardingController implements OnboardingApi {

    private final OnboardingService onboardingService;

    private static final CustomerOnboardingDTOToCustomerOnboardingMapper customerOnboardingDTOToCustomerOnboardingMapper =
            CustomerOnboardingDTOToCustomerOnboardingMapper.MAPPER;

    @CustomerAccess(CustomerAccessLevel.READ)
    @Override
    public ResponseEntity<CustomerOnboardingDTO> getActiveOnboarding(UUID customerId) {
        final CustomerOnboarding activeOnboarding = onboardingService.getActiveOnboarding(customerId);
        final CustomerOnboardingDTO customerOnboardingDTO = customerOnboardingDTOToCustomerOnboardingMapper
                .toCustomerOnboardingDTO(activeOnboarding);
        return ResponseEntity.ok(customerOnboardingDTO);
    }

    @CustomerAccess(CustomerAccessLevel.READ)
    @Override
    public ResponseEntity<CustomerOnboardingDTO> getOnboardingById(UUID customerId, Long onboardingId) {
        final CustomerOnboarding onboarding = onboardingService.getOnboardingById(customerId, onboardingId);
        final CustomerOnboardingDTO customerOnboardingDTO = customerOnboardingDTOToCustomerOnboardingMapper
                .toCustomerOnboardingDTO(onboarding);
        return ResponseEntity.ok(customerOnboardingDTO);
    }

    @CustomerAccess(CustomerAccessLevel.READ)
    @Override
    public ResponseEntity<List<CustomerOnboardingDTO>> getOnboardingInstances(UUID customerId) {
        final List<CustomerOnboarding> onboardingInstances = onboardingService.getOnboardingInstances(customerId);
        final List<CustomerOnboardingDTO> customerOnboardingDTOs = onboardingInstances
                .stream()
                .map(customerOnboardingDTOToCustomerOnboardingMapper::toCustomerOnboardingDTO)
                .toList();
        return ResponseEntity.ok(customerOnboardingDTOs);
    }
}
