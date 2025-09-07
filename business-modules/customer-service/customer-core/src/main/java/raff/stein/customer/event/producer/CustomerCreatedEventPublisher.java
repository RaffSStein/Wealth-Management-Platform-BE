package raff.stein.customer.event.producer;

import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.CustomerCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import raff.stein.customer.event.producer.mapper.CustomerCreatedMapper;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.platformcore.messaging.publisher.WMPBaseEventPublisher;
import raff.stein.platformcore.messaging.publisher.model.EventData;

import java.util.UUID;

@Slf4j
@Component
public class CustomerCreatedEventPublisher {

    private final WMPBaseEventPublisher wmpBaseEventPublisher;
    private final String customerCreatedTopic;
    private final CustomerRepository customerRepository;

    private static final CustomerCreatedMapper customerCreatedMapper = CustomerCreatedMapper.MAPPER;

    public CustomerCreatedEventPublisher(
            WMPBaseEventPublisher wmpBaseEventPublisher,
            @Value("${kafka.topics.customer-service.customer-created.name}") String customerCreatedTopic,
            CustomerRepository customerRepository) {
        this.wmpBaseEventPublisher = wmpBaseEventPublisher;
        this.customerCreatedTopic = customerCreatedTopic;
        this.customerRepository = customerRepository;
    }

    public void publishCustomerCreatedEvent(UUID customerId) {
        final CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer with ID " + customerId + " not found"));
        final CustomerCreatedEvent customerCreatedEvent =
                customerCreatedMapper.toCustomerCreatedEvent(customerEntity);
        EventData eventData = new EventData(customerCreatedEvent);
        wmpBaseEventPublisher.publishCloudEvent(customerCreatedTopic, eventData);
    }

}
