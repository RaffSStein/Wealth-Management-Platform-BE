package raff.stein.customer.controller.mapper.onboarding;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import io.openapiprocessor.openapi.model.CustomerOnboardingDto;
import raff.stein.customer.controller.mapper.CustomerControllerCommonMapperConfig;
import raff.stein.customer.model.bo.onboarding.CustomerOnboarding;

@Mapper(config = CustomerControllerCommonMapperConfig.class)
public interface CustomerOnboardingDTOToCustomerOnboardingMapper {

    CustomerOnboardingDTOToCustomerOnboardingMapper MAPPER = Mappers.getMapper(CustomerOnboardingDTOToCustomerOnboardingMapper.class);

    CustomerOnboardingDto toCustomerOnboardingDTO(CustomerOnboarding customerOnboarding);

    CustomerOnboarding toCustomerOnboarding(CustomerOnboardingDto customerOnboardingDTO);
}
