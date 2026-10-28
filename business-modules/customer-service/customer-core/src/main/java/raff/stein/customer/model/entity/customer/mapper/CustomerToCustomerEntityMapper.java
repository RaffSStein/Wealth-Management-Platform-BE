package raff.stein.customer.model.entity.customer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.bo.mifid.filling.CustomerRiskProfile;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.mapper.CustomerEntityCommonMapperConfig;

@Mapper(
        config = CustomerEntityCommonMapperConfig.class,
        uses = {
                CustomerGoalsToCustomerGoalsEntityMapper.class,
                CustomerFinancialsToCustomerFinancialEntityMapper.class
        })
public interface CustomerToCustomerEntityMapper {

    CustomerToCustomerEntityMapper MAPPER = Mappers.getMapper(CustomerToCustomerEntityMapper.class);

    @Mapping(target = "id", ignore = true)
    // the risk profile is only ever set by the MiFID submit, never from a customer payload
    @Mapping(target = "riskProfile", ignore = true)
    @Mapping(target = "riskScore", ignore = true)
    CustomerEntity toCustomerEntity(Customer customer);

    @Mapping(target = "customerGoals", source = "customerFinancialGoals")
    @Mapping(target = "riskProfile", source = "customerEntity", qualifiedByName = "toCustomerRiskProfile")
    Customer toCustomer(CustomerEntity customerEntity);

    // null when no MiFID filling was submitted yet, so the DTO keeps riskProfile empty instead of {profile: null}
    @Named("toCustomerRiskProfile")
    default CustomerRiskProfile toCustomerRiskProfile(CustomerEntity customerEntity) {
        if (customerEntity == null || customerEntity.getRiskProfile() == null) {
            return null;
        }
        return CustomerRiskProfile.builder()
                .profile(customerEntity.getRiskProfile())
                .score(customerEntity.getRiskScore())
                .build();
    }
}
