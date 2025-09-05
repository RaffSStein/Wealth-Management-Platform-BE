package raff.stein.customer.model.entity.onboarding.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.model.bo.onboarding.CustomerOnboarding;
import raff.stein.customer.model.entity.mapper.CustomerEntityCommonMapperConfig;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;

@Mapper(config = CustomerEntityCommonMapperConfig.class)
public interface CustomerOnboardingToCustomerOnboardingEntityMapper {

    CustomerOnboardingToCustomerOnboardingEntityMapper MAPPER = Mappers.getMapper(CustomerOnboardingToCustomerOnboardingEntityMapper.class);

    CustomerOnboarding toCustomerOnboarding(CustomerOnboardingEntity customerOnboardingEntity);

    CustomerOnboardingEntity toCustomerOnboardingEntity(CustomerOnboarding customerOnboarding);

}
