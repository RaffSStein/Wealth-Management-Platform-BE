package raff.stein.customer.model.entity.customer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.model.bo.customer.CustomerFinancials;
import raff.stein.customer.model.entity.customer.CustomerFinancialEntity;
import raff.stein.customer.model.entity.financial.mapper.FinancialTypeToFinancialTypeEntityMapper;
import raff.stein.customer.model.entity.mapper.CustomerEntityCommonMapperConfig;

@Mapper(
        config = CustomerEntityCommonMapperConfig.class,
        uses = {FinancialTypeToFinancialTypeEntityMapper.class})
public interface CustomerFinancialsToCustomerFinancialEntityMapper {

    CustomerFinancialsToCustomerFinancialEntityMapper MAPPER = Mappers.getMapper(CustomerFinancialsToCustomerFinancialEntityMapper.class);

    @Mapping(target = "financialType", ignore = true)
    @Mapping(target = "customer", ignore = true)
    CustomerFinancialEntity toCustomerFinancialsEntity(CustomerFinancials customerFinancials);

    CustomerFinancials toCustomerFinancials(CustomerFinancialEntity customerFinancialEntity);
}
