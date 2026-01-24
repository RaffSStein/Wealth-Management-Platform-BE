package raff.stein.customer.controller.mapper.customer;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.*;
import raff.stein.customer.controller.mapper.CustomerControllerCommonMapperConfig;
import raff.stein.customer.controller.mapper.financial.FinancialTypeDTOToFinancialType;
import raff.stein.customer.controller.mapper.goal.GoalTypeDTOToGoalTypeMapper;
import raff.stein.customer.model.bo.customer.Customer;
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

    CustomerDTO toCustomerDTO(Customer customer);

    Customer toCustomer(CustomerDTO customerDTO);

    @Mapping(target = "gender", source = "gender", qualifiedByName = "stringToGender")
    CustomerPersonalDetailsDTO toCustomerPersonalDetailsDTO(CustomerPersonalDetails customerPersonalDetails);

    @Mapping(target = "gender", source = "gender", qualifiedByName = "genderToString")
    CustomerPersonalDetails toCustomerPersonalDetails(CustomerPersonalDetailsDTO customerPersonalDetailsDTO);

    // Financials mapping methods

    CustomerFinancialDTO toCustomerFinancialDTO(CustomerFinancials customerFinancials);

    CustomerFinancials toCustomerFinancials(CustomerFinancialDTO customerFinancialDTO);

    // Goals mapping methods

    CustomerGoalDTO toCustomerGoalDTO(CustomerGoals customerGoals);

    CustomerGoals toCustomerGoals(CustomerGoalDTO customerGoalDTO);

    // Helpers

    @Named("genderToString")
    default String genderToString(Gender gender) {
        return gender == null ? null : gender.name();
    }

    @Named("stringToGender")
    default Gender stringToGender(String gender) {
        return (gender == null || gender.isBlank()) ? null : Gender.valueOf(gender);
    }
}
