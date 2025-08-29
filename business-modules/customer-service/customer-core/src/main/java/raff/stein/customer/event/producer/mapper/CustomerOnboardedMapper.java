package raff.stein.customer.event.producer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.CustomerOnboardedEvent;
import raff.stein.customer.event.mapper.config.CustomerEventMapperConfig;
import raff.stein.customer.model.bo.mifid.filling.CustomerRiskProfile;
import raff.stein.customer.model.entity.customer.CustomerEntity;

@Mapper(config = CustomerEventMapperConfig.class)
public interface CustomerOnboardedMapper {

    CustomerOnboardedMapper MAPPER = Mappers.getMapper(CustomerOnboardedMapper.class);

    @Mapping(target = "customer.customerFinancials", ignore = true)
    @Mapping(target = "customer.customerGoals", ignore = true)
    CustomerOnboardedEvent toCustomerOnboardedEvent(CustomerRiskProfile customerRiskProfile, CustomerEntity customer);

}
