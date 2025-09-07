package raff.stein.customer.service.aml.pipeline.step.ue.impl.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.client.eu.sanctions.model.screen.ScreenRequest;
import raff.stein.customer.event.mapper.config.CustomerEventMapperConfig;
import raff.stein.customer.model.bo.customer.Customer;

@Mapper(config = CustomerEventMapperConfig.class)
public interface SanctionRequestMapper {

    SanctionRequestMapper MAPPER = Mappers.getMapper(SanctionRequestMapper.class);

    ScreenRequest toScreenRequest(Customer customer);
}
