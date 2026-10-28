package raff.stein.customer.model.entity.onboarding.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.model.bo.onboarding.CustomerOnboarding;
import raff.stein.customer.model.bo.onboarding.CustomerOnboardingStep;
import raff.stein.customer.model.entity.mapper.CustomerEntityCommonMapperConfig;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;

import java.util.Comparator;

@Mapper(config = CustomerEntityCommonMapperConfig.class)
public interface CustomerOnboardingToCustomerOnboardingEntityMapper {

    CustomerOnboardingToCustomerOnboardingEntityMapper MAPPER = Mappers.getMapper(CustomerOnboardingToCustomerOnboardingEntityMapper.class);

    // names differ between entity and BO: without this the steps were silently dropped
    @Mapping(target = "steps", source = "onboardingSteps")
    CustomerOnboarding toCustomerOnboarding(CustomerOnboardingEntity customerOnboardingEntity);

    @Mapping(target = "onboardingSteps", source = "steps")
    CustomerOnboardingEntity toCustomerOnboardingEntity(CustomerOnboarding customerOnboarding);

    // steps are stored in a Set: return them in process order
    @AfterMapping
    default void sortSteps(@MappingTarget CustomerOnboarding customerOnboarding) {
        if (customerOnboarding.getSteps() != null) {
            customerOnboarding.getSteps().sort(Comparator.comparingInt(CustomerOnboardingStep::getStepOrder));
        }
    }
}
