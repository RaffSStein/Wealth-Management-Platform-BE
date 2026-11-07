package raff.stein.customer.service.aml;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import raff.stein.customer.event.producer.CustomerCreatedEventPublisher;
import raff.stein.customer.event.producer.CustomerRejectedEventPublisher;
import raff.stein.customer.exception.CustomerNotFoundException;
import raff.stein.customer.model.bo.aml.AmlVerification;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.entity.aml.AmlVerificationEntity;
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
import raff.stein.customer.service.onboarding.handler.impl.AmlStepHandler;

import java.time.ZoneOffset;
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
    private final TransactionTemplate transactionTemplate;

    private static final AmlVerificationToAmlVerificationEntityMapper amlVerificationMapper = AmlVerificationToAmlVerificationEntityMapper.MAPPER;
    private static final CustomerToCustomerEntityMapper customerMapper = CustomerToCustomerEntityMapper.MAPPER;

    //TODO: add API for AML retrigger
    public void triggerAmlCheck(@NonNull UUID customerId) {
        final CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(CustomerNotFoundException.with(customerId));
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
                // OffsetDateTime.from(Instant) always throws: an Instant has no offset, so pin it to UTC
                .verificationDate(amlResult.finishedAt().atOffset(ZoneOffset.UTC))
                .status(amlResult.overallStatus().name())
                .countryCode(customer.getCountry())
                .verificationResult(amlResult.overallStatus().name())
                .amlResult(amlResult)
                .build();

        // the pipeline calls external providers, so it runs above, outside any transaction. What follows is one unit:
        // the verification, the onboarding step and the outcome event are stored together or not at all
        transactionTemplate.executeWithoutResult(status -> saveOutcomeAndPublish(amlVerification));
    }

    private void saveOutcomeAndPublish(final AmlVerification amlVerification) {
        final Customer customer = amlVerification.getCustomer();
        final AmlVerificationEntity amlVerificationEntity = amlVerificationMapper.toAmlVerificationEntity(amlVerification);
        // the customer comes from the event payload: reference the persisted row by id instead of saving a copy of it
        amlVerificationEntity.setCustomer(customerRepository.getReferenceById(customer.getId()));
        amlVerificationRepository.save(amlVerificationEntity);

        // Proceed with onboarding based on AML result
        onboardingService.proceedToStep(
                OnboardingStep.AML,
                OnboardingStepContext.builder()
                        .customerId(customer.getId())
                        .metadata(Map.of(
                                "amlCaseId", amlVerification.getAmlCaseId(),
                                AmlStepHandler.AML_RESULT_METADATA_KEY, amlVerification.getAmlResult()))
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
