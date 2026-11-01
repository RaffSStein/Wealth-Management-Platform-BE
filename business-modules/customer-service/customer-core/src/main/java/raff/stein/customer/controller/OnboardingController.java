package raff.stein.customer.controller;

import lombok.RequiredArgsConstructor;
import io.openapiprocessor.openapi.api.OnboardingApi;
import io.openapiprocessor.openapi.model.CustomerOnboardingDto;
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
    public ResponseEntity<CustomerOnboardingDto> getActiveOnboarding(UUID customerId) {
        final CustomerOnboarding activeOnboarding = onboardingService.getActiveOnboarding(customerId);
        final CustomerOnboardingDto customerOnboardingDTO = customerOnboardingDTOToCustomerOnboardingMapper
                .toCustomerOnboardingDTO(activeOnboarding);
        return ResponseEntity.ok(customerOnboardingDTO);
    }

    @CustomerAccess(CustomerAccessLevel.READ)
    @Override
    public ResponseEntity<CustomerOnboardingDto> getOnboardingById(UUID customerId, Long onboardingId) {
        final CustomerOnboarding onboarding = onboardingService.getOnboardingById(customerId, onboardingId);
        final CustomerOnboardingDto customerOnboardingDTO = customerOnboardingDTOToCustomerOnboardingMapper
                .toCustomerOnboardingDTO(onboarding);
        return ResponseEntity.ok(customerOnboardingDTO);
    }

    @CustomerAccess(CustomerAccessLevel.READ)
    @Override
    public ResponseEntity<List<CustomerOnboardingDto>> getOnboardingInstances(UUID customerId) {
        final List<CustomerOnboarding> onboardingInstances = onboardingService.getOnboardingInstances(customerId);
        final List<CustomerOnboardingDto> customerOnboardingDTOs = onboardingInstances
                .stream()
                .map(customerOnboardingDTOToCustomerOnboardingMapper::toCustomerOnboardingDTO)
                .toList();
        return ResponseEntity.ok(customerOnboardingDTOs);
    }
}
