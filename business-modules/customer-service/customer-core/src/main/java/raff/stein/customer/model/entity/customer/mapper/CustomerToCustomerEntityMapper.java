package raff.stein.customer.model.entity.customer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.model.bo.customer.Customer;
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
    CustomerEntity toCustomerEntity(Customer customer);

    @Mapping(target = "customerGoals", source = "customerFinancialGoals")
    Customer toCustomer(CustomerEntity customerEntity);
}
