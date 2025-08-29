package raff.stein.customer.event.producer;

import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.CustomerOnboardedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import raff.stein.customer.event.producer.mapper.CustomerOnboardedMapper;
import raff.stein.customer.model.bo.mifid.filling.CustomerRiskProfile;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.platformcore.messaging.publisher.WMPBaseEventPublisher;
import raff.stein.platformcore.messaging.publisher.model.EventData;

import java.util.UUID;

@Slf4j
@Component
public class CustomerOnboardedEventPublisher {

    private final WMPBaseEventPublisher wmpBaseEventPublisher;
    private final String customerOnboardedTopic;
    private final CustomerRepository customerRepository;
    private static final CustomerOnboardedMapper customerOnboardedMapper = CustomerOnboardedMapper.MAPPER;

    public CustomerOnboardedEventPublisher(
            WMPBaseEventPublisher wmpBaseEventPublisher,
            @Value("${kafka.topics.customer-service.customer-onboarded.name}") String customerOnboardedTopic,
            CustomerRepository customerRepository) {
        this.wmpBaseEventPublisher = wmpBaseEventPublisher;
        this.customerOnboardedTopic = customerOnboardedTopic;
        this.customerRepository = customerRepository;
    }

    public void publishCustomerOnboardedEvent(CustomerRiskProfile customerRiskProfile, UUID customerId) {
        final CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer with ID " + customerId + " not found"));
        CustomerOnboardedEvent documentUploadedEvent =
                customerOnboardedMapper.toCustomerOnboardedEvent(customerRiskProfile, customerEntity);
        EventData eventData = new EventData(documentUploadedEvent);
        wmpBaseEventPublisher.publishCloudEvent(customerOnboardedTopic, eventData);
    }

}
