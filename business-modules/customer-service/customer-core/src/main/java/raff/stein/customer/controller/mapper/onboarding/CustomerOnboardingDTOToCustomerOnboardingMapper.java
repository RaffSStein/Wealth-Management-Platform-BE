package raff.stein.customer.controller.mapper.onboarding;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.CustomerOnboardingDTO;
import raff.stein.customer.controller.mapper.CustomerControllerCommonMapperConfig;
import raff.stein.customer.model.bo.onboarding.CustomerOnboarding;

@Mapper(config = CustomerControllerCommonMapperConfig.class)
public interface CustomerOnboardingDTOToCustomerOnboardingMapper {

    CustomerOnboardingDTOToCustomerOnboardingMapper MAPPER = Mappers.getMapper(CustomerOnboardingDTOToCustomerOnboardingMapper.class);

    CustomerOnboardingDTO toCustomerOnboardingDTO(CustomerOnboarding customerOnboarding);

    CustomerOnboarding toCustomerOnboarding(CustomerOnboardingDTO customerOnboardingDTO);
}
