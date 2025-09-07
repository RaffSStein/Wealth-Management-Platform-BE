package raff.stein.customer.event.producer;

import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.CustomerRejectedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import raff.stein.customer.event.producer.mapper.CustomerRejectedMapper;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.platformcore.messaging.publisher.WMPBaseEventPublisher;
import raff.stein.platformcore.messaging.publisher.model.EventData;

import java.util.UUID;

@Slf4j
@Component
public class CustomerRejectedEventPublisher {

    private final WMPBaseEventPublisher wmpBaseEventPublisher;
    private final String customerRejectedTopic;
    private final CustomerRepository customerRepository;

    private static final CustomerRejectedMapper customerRejectedMapper = CustomerRejectedMapper.MAPPER;

    public CustomerRejectedEventPublisher(
            WMPBaseEventPublisher wmpBaseEventPublisher,
            @Value("${kafka.topics.customer-service.customer-rejected.name}") String customerRejectedTopic,
            CustomerRepository customerRepository) {
        this.wmpBaseEventPublisher = wmpBaseEventPublisher;
        this.customerRejectedTopic = customerRejectedTopic;
        this.customerRepository = customerRepository;
    }

    public void publishCustomerRejectedEvent(UUID customerId) {
        final CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer with ID " + customerId + " not found"));
        final CustomerRejectedEvent customerCreatedEvent =
                customerRejectedMapper.toCustomerRejectedEvent(customerEntity);
        EventData eventData = new EventData(customerCreatedEvent);
        wmpBaseEventPublisher.publishCloudEvent(customerRejectedTopic, eventData);
    }

}
