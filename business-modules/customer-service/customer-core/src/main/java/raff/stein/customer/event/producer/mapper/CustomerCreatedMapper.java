package raff.stein.customer.event.producer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.CustomerCreatedEvent;
import raff.stein.customer.event.mapper.config.CustomerEventMapperConfig;
import raff.stein.customer.model.entity.customer.CustomerEntity;

@Mapper(config = CustomerEventMapperConfig.class)
public interface CustomerCreatedMapper {

    CustomerCreatedMapper MAPPER = Mappers.getMapper(CustomerCreatedMapper.class);

    @Mapping(target = "customer.customerFinancials", ignore = true)
    @Mapping(target = "customer.customerGoals", ignore = true)
    CustomerCreatedEvent toCustomerCreatedEvent(CustomerEntity customer);

}
