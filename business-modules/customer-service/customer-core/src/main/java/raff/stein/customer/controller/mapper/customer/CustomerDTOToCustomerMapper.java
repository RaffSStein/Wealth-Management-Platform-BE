package raff.stein.customer.controller.mapper.customer;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.CustomerDTO;
import org.openapitools.model.CustomerFinancialDTO;
import org.openapitools.model.CustomerGoalDTO;
import raff.stein.customer.controller.mapper.CustomerControllerCommonMapperConfig;
import raff.stein.customer.controller.mapper.financial.FinancialTypeDTOToFinancialType;
import raff.stein.customer.controller.mapper.goal.GoalTypeDTOToGoalTypeMapper;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.bo.customer.CustomerFinancials;
import raff.stein.customer.model.bo.customer.CustomerGoals;

@Mapper(
        config = CustomerControllerCommonMapperConfig.class,
        uses = {
                FinancialTypeDTOToFinancialType.class,
                GoalTypeDTOToGoalTypeMapper.class
        })
public interface CustomerDTOToCustomerMapper {

    CustomerDTOToCustomerMapper MAPPER = Mappers.getMapper(CustomerDTOToCustomerMapper.class);

    CustomerDTO toCustomerDTO(Customer customer);

    Customer toCustomer(CustomerDTO customerDTO);

    // Financials mapping methods

    CustomerFinancialDTO toCustomerFinancialDTO(CustomerFinancials customerFinancials);

    CustomerFinancials toCustomerFinancials(CustomerFinancialDTO customerFinancialDTO);

    // Goals mapping methods

    CustomerGoalDTO toCustomerGoalDTO(CustomerGoals customerGoals);

    CustomerGoals toCustomerGoals(CustomerGoalDTO customerGoalDTO);


}
