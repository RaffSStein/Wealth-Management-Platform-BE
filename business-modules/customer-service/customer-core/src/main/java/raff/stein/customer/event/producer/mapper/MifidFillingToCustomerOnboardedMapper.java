package raff.stein.customer.event.producer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.CustomerOnboardedEvent;
import org.openapitools.model.DocumentUploadedEvent;
import raff.stein.customer.event.mapper.config.CustomerEventMapperConfig;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;

import java.util.UUID;

@Mapper(config = CustomerEventMapperConfig.class)
public interface MifidFillingToCustomerOnboardedMapper {

    MifidFillingToCustomerOnboardedMapper MAPPER = Mappers.getMapper(MifidFillingToCustomerOnboardedMapper.class);

    CustomerOnboardedEvent toCustomerOnboardedEvent(MifidFilling mifidFilling, UUID customerId);

    MifidFilling toMifidFilling(DocumentUploadedEvent documentUploadedEvent);
}
