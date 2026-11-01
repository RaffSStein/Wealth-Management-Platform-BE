package raff.stein.customer.controller.mapper.customer;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import io.openapiprocessor.openapi.model.*;
import raff.stein.customer.controller.mapper.CustomerControllerCommonMapperConfig;
import raff.stein.customer.controller.mapper.financial.FinancialTypeDTOToFinancialType;
import raff.stein.customer.controller.mapper.goal.GoalTypeDTOToGoalTypeMapper;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.bo.customer.CustomerSummary;
import raff.stein.customer.model.bo.dashboard.AdvisorDashboardStatistics;
import raff.stein.customer.model.bo.customer.CustomerFinancials;
import raff.stein.customer.model.bo.customer.CustomerGoals;
import raff.stein.customer.model.bo.customer.CustomerPersonalDetails;

@Mapper(
        config = CustomerControllerCommonMapperConfig.class,
        uses = {
                FinancialTypeDTOToFinancialType.class,
                GoalTypeDTOToGoalTypeMapper.class
        })
public interface CustomerDTOToCustomerMapper {

    CustomerDTOToCustomerMapper MAPPER = Mappers.getMapper(CustomerDTOToCustomerMapper.class);

    CustomerDto toCustomerDTO(Customer customer);

    // the branch comes from the token, never from a request
    @Mapping(target = "branchCode", ignore = true)
    Customer toCustomer(CustomerDto customerDTO);

    CustomerSummaryDto toCustomerSummaryDTO(CustomerSummary customerSummary);

    AdvisorDashboardStatisticsDto toAdvisorDashboardStatisticsDto(AdvisorDashboardStatistics statistics);

    CustomerPersonalDetailsDto toCustomerPersonalDetailsDTO(CustomerPersonalDetails customerPersonalDetails);

    CustomerPersonalDetails toCustomerPersonalDetails(CustomerPersonalDetailsDto customerPersonalDetailsDTO);

    // Financials mapping methods

    CustomerFinancialDto toCustomerFinancialDTO(CustomerFinancials customerFinancials);

    CustomerFinancials toCustomerFinancials(CustomerFinancialDto customerFinancialDTO);

    // Goals mapping methods

    CustomerGoalDto toCustomerGoalDTO(CustomerGoals customerGoals);

    CustomerGoals toCustomerGoals(CustomerGoalDto customerGoalDTO);
}
