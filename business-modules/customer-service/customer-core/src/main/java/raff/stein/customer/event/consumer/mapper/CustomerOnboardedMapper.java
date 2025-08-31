package raff.stein.customer.event.consumer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.event.mapper.config.CustomerEventMapperConfig;
import raff.stein.customer.model.bo.customer.Customer;

@Mapper(config = CustomerEventMapperConfig.class)
public interface CustomerOnboardedMapper {

    CustomerOnboardedMapper MAPPER = Mappers.getMapper(CustomerOnboardedMapper.class);

    Customer toCustomer(org.openapitools.model.Customer customer);
}
