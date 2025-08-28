package raff.stein.customer.event.consumer;

import io.cloudevents.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.FileValidatedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.service.onboarding.OnboardingService;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;
import raff.stein.platformcore.messaging.consumer.WMPBaseEventConsumer;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.topics.customer-service.document.file-validated.enabled", havingValue = "true")
public class FileValidatedEventConsumer extends WMPBaseEventConsumer {

    private final CustomerRepository customerRepository;
    private final OnboardingService onboardingService;

    @KafkaListener(
            topics = "${kafka.topics.customer-service.document.file-validated.name}",
            containerFactory = "kafkaListenerFactory",
            groupId = "${kafka.topics.customer-service.document.file-validated.groupId}")
    public void consume(CloudEvent cloudEvent) {
        withEventPayload(
                cloudEvent,
                FileValidatedEvent.class,
                payload -> processFileValidatedEvent(payload, cloudEvent.getId()));
    }

    private void processFileValidatedEvent(FileValidatedEvent fileValidatedEvent, String eventId) {
        log.info("Processing FileValidatedEvent with eventId: [{}]", eventId);
        final UUID customerId = fileValidatedEvent.getCustomerId();
        if (customerId == null) {
            log.warn("FileValidatedEvent with eventId: [{}] has no customer ID. Skipping processing.", eventId);
            return;
        }
        // Check if the customer exists
        if (!customerRepository.existsById(customerId)) {
            log.warn("Customer with ID: [{}] does not exist. Skipping processing for eventId: [{}]", customerId, eventId);
            return;
        }
        final Boolean isValid = fileValidatedEvent.getIsValid();
        if (isValid == null) {
            log.warn("FileValidatedEvent with eventId: [{}] has no validation result. Skipping processing.", eventId);
            return;
        }
        if (isValid) {
            log.info("File with ID [{}] is valid for customer with ID: [{}]. Proceeding with onboarding step update.",
                    fileValidatedEvent.getFileId(),
                    customerId);
        } else {
            log.warn("File with ID [{}] is not valid for customer with ID: [{}]. Proceeding with onboarding step update.",
                    fileValidatedEvent.getFileId(),
                    customerId);
        }
        // proceed with step update
        onboardingService.proceedToStep(
                OnboardingStep.DOCUMENTS,
                OnboardingStepContext.builder()
                        .customerId(customerId)
                        .metadata(Map.of(
                                "fileId", fileValidatedEvent.getFileId(),
                                "isValid", isValid))
                        .build());

        log.info("FileValidatedEvent with eventId: [{}] processed successfully", eventId);
    }

}
