package raff.stein.customer.event.consumer;

import io.cloudevents.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.CustomerRejectedEvent;
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
@ConditionalOnProperty(name = "kafka.topics.customer-service.customer-rejected.consuming-enabled", havingValue = "true")
public class CustomerRejectedEventConsumer extends WMPBaseEventConsumer {

    private final OnboardingService onboardingService;

    @KafkaListener(
            topics = "${kafka.topics.customer-service.customer-rejected.name}",
            containerFactory = "kafkaListenerFactory",
            groupId = "${kafka.topics.customer-service.customer-rejected.groupId}")
    public void consume(CloudEvent cloudEvent) {
        withEventPayload(
                cloudEvent,
                CustomerRejectedEvent.class,
                payload -> processCustomerRejectedEvent(payload, cloudEvent.getId()));
    }

    private void processCustomerRejectedEvent(CustomerRejectedEvent customerRejectedEvent, String eventId) {
        log.info("Received CustomerRejectedEvent with id: {} and eventId: {}",
                customerRejectedEvent.getCustomer().getId(),
                eventId);
        onboardingService.proceedToStep(
                OnboardingStep.FINALIZE,
                OnboardingStepContext.builder()
                        .customerId(customerRejectedEvent.getCustomer().getId())
                        .build());

    }

}
