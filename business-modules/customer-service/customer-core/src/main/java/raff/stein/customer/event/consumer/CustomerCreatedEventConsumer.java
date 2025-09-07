package raff.stein.customer.event.consumer;

import io.cloudevents.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.CustomerCreatedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.service.onboarding.OnboardingService;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;
import raff.stein.platformcore.messaging.consumer.WMPBaseEventConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.topics.customer-service.customer-created.consuming-enabled", havingValue = "true")
public class CustomerCreatedEventConsumer extends WMPBaseEventConsumer {

    private final OnboardingService onboardingService;

    @KafkaListener(
            topics = "${kafka.topics.customer-service.customer-created.name}",
            containerFactory = "kafkaListenerFactory",
            groupId = "${kafka.topics.customer-service.customer-created.groupId}")
    public void consume(CloudEvent cloudEvent) {
        withEventPayload(
                cloudEvent,
                CustomerCreatedEvent.class,
                payload -> processCustomerCreatedEvent(payload, cloudEvent.getId()));
    }

    private void processCustomerCreatedEvent(CustomerCreatedEvent customerCreatedEvent, String eventId) {
        log.info("Received CustomerCreatedEvent with id: {} and eventId: {}",
                customerCreatedEvent.getCustomer().getId(),
                eventId);
        onboardingService.proceedToStep(
                OnboardingStep.FINALIZE,
                OnboardingStepContext.builder()
                        .customerId(customerCreatedEvent.getCustomer().getId())
                        .build());

    }

}
