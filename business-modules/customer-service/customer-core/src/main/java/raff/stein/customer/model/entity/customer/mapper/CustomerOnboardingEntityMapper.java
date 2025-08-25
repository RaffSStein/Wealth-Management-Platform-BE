package raff.stein.customer.model.entity.customer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.model.entity.mapper.CustomerEntityCommonMapperConfig;

@Mapper(config = CustomerEntityCommonMapperConfig.class)
public interface CustomerOnboardingEntityMapper {

    CustomerOnboardingEntityMapper MAPPER = Mappers.getMapper(CustomerOnboardingEntityMapper.class);

    // Define mapping methods here if needed

}
