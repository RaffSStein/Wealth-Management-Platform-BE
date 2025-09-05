package raff.stein.customer.service.aml;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import raff.stein.customer.event.producer.CustomerCreatedEventPublisher;
import raff.stein.customer.event.producer.CustomerRejectedEventPublisher;
import raff.stein.customer.model.bo.aml.AmlVerification;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.entity.aml.mapper.AmlVerificationToAmlVerificationEntityMapper;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.customer.mapper.CustomerToCustomerEntityMapper;
import raff.stein.customer.repository.aml.AmlVerificationRepository;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.service.aml.pipeline.AmlPipelineExecutor;
import raff.stein.customer.service.aml.pipeline.AmlResult;
import raff.stein.customer.service.aml.pipeline.step.AmlContext;
import raff.stein.customer.service.aml.pipeline.step.AmlStepResult;
import raff.stein.customer.service.aml.utils.JurisdictionUtils;
import raff.stein.customer.service.onboarding.OnboardingService;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmlService {

    private final AmlPipelineExecutor amlPipelineExecutor;
    private final AmlVerificationRepository amlVerificationRepository;
    private final OnboardingService onboardingService;
    private final CustomerCreatedEventPublisher customerCreatedEventPublisher;
    private final CustomerRejectedEventPublisher customerRejectedEventPublisher;
    private final CustomerRepository customerRepository;

    private static final AmlVerificationToAmlVerificationEntityMapper amlVerificationMapper = AmlVerificationToAmlVerificationEntityMapper.MAPPER;
    private static final CustomerToCustomerEntityMapper customerMapper = CustomerToCustomerEntityMapper.MAPPER;

    public void triggerAmlCheck(@NonNull UUID customerId) {
        final CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer with ID " + customerId + " not found"));
        Customer customer = customerMapper.toCustomer(customerEntity);
        triggerAmlCheck(customer);
    }

    public void triggerAmlCheck(@NonNull Customer customer) {
        log.info("Triggering AML check for customer {} ", customer.getId());
        final String jurisdiction = JurisdictionUtils.resolveJurisdiction(customer.getCountry())
                .name()
                .toLowerCase(Locale.ROOT);
        log.info("Resolved jurisdiction {} for customer {} with country {} ",
                jurisdiction,
                customer.getId(),
                customer.getCountry());
        AmlContext context = AmlContext.builder()
                .customer(customer)
                .jurisdiction(jurisdiction)
                .amlCaseId(UUID.randomUUID())
                .build();

        final AmlResult amlResult = amlPipelineExecutor.execute(context);
        log.info("Completed AML check for customer {} with overall status: {} ", customer.getId(), amlResult.overallStatus());

        // Build BO AmlVerification from pipeline output
        AmlVerification amlVerification = AmlVerification.builder()
                .amlCaseId(context.getAmlCaseId())
                .customer(customer)
                .jurisdiction(jurisdiction)
                .verificationDate(OffsetDateTime.from(amlResult.finishedAt()))
                .status(amlResult.overallStatus().name())
                .countryCode(customer.getCountry())
                .verificationResult(amlResult.overallStatus().name())
                .amlResult(amlResult)
                .build();

        amlVerificationRepository.save(
                amlVerificationMapper.toAmlVerificationEntity(amlVerification));

        // Proceed with onboarding based on AML result
        onboardingService.proceedToStep(
                OnboardingStep.AML,
                OnboardingStepContext.builder()
                        .customerId(amlVerification.getCustomer().getId())
                        .metadata(Map.of(
                                "amlCaseId", amlVerification.getAmlCaseId(),
                                "AmlResult", amlVerification.getAmlResult()))
                        .build());

        // if AML verification is passed, publish CustomerCreatedEvent, otherwise publish CustomerRejectedEvent
        if(amlVerification.getAmlResult().overallStatus().equals(AmlStepResult.StepStatus.PASSED)) {
            log.info("AML check passed for customer {}. Proceeding with onboarding.", customer.getId());
            customerCreatedEventPublisher.publishCustomerCreatedEvent(customer.getId());
        } else {
            log.warn("AML check did not pass for customer {}. Further action may be required.", customer.getId());
            customerRejectedEventPublisher.publishCustomerRejectedEvent(customer.getId());
        }

    }

}
