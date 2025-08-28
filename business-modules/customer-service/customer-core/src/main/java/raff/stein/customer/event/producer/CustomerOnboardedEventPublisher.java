package raff.stein.customer.event.producer;

import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.CustomerOnboardedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import raff.stein.customer.event.producer.mapper.MifidFillingToCustomerOnboardedMapper;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.platformcore.messaging.publisher.WMPBaseEventPublisher;
import raff.stein.platformcore.messaging.publisher.model.EventData;

import java.util.UUID;

@Slf4j
@Component
public class CustomerOnboardedEventPublisher {

    private final WMPBaseEventPublisher wmpBaseEventPublisher;
    private final String customerOnboardedTopic;
    private static final MifidFillingToCustomerOnboardedMapper documentToDocumentUploadedEventMapper = MifidFillingToCustomerOnboardedMapper.MAPPER;

    public CustomerOnboardedEventPublisher(
            WMPBaseEventPublisher wmpBaseEventPublisher,
            @Value("${kafka.topics.customer-service.customer-onboarded.name}") String customerOnboardedTopic) {
        this.wmpBaseEventPublisher = wmpBaseEventPublisher;
        this.customerOnboardedTopic = customerOnboardedTopic;
    }

    public void publishCustomerOnboardedEvent(MifidFilling mifidFilling, UUID customerId) {
        CustomerOnboardedEvent documentUploadedEvent =
                documentToDocumentUploadedEventMapper.toCustomerOnboardedEvent(mifidFilling, customerId);
        EventData eventData = new EventData(documentUploadedEvent);
        wmpBaseEventPublisher.publishCloudEvent(customerOnboardedTopic, eventData);
    }

}
