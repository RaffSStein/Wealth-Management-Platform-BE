package raff.stein.customer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.CustomerApi;
import org.openapitools.model.CustomerDTO;
import org.openapitools.model.CustomerFinancialDTO;
import org.openapitools.model.CustomerGoalDTO;
import org.openapitools.model.CustomerPersonalDetailsDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import raff.stein.customer.controller.mapper.customer.CustomerDTOToCustomerMapper;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.bo.customer.CustomerFinancials;
import raff.stein.customer.model.bo.customer.CustomerGoals;
import raff.stein.customer.model.bo.customer.CustomerPersonalDetails;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.service.CustomerService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CustomerController implements CustomerApi {

    private final CustomerService customerService;

    private static final CustomerDTOToCustomerMapper customerDTOToCustomerMapper = CustomerDTOToCustomerMapper.MAPPER;

    @Override
    public ResponseEntity<CustomerDTO> initCustomer(CustomerDTO customerDTO) {
        Customer customerInput = customerDTOToCustomerMapper.toCustomer(customerDTO);
        Customer createdCustomer = customerService.initCustomer(customerInput);
        CustomerDTO responseCustomerDTO = customerDTOToCustomerMapper.toCustomerDTO(createdCustomer);
        return ResponseEntity.ok(responseCustomerDTO);
    }

    @Override
    public ResponseEntity<CustomerDTO> updateCustomerPersonalDetails(UUID customerId, CustomerPersonalDetailsDTO customerPersonalDetailsDTO) {
        CustomerPersonalDetails customerPersonalDetails = customerDTOToCustomerMapper.toCustomerPersonalDetails(customerPersonalDetailsDTO);
        // update customer personal details
        Customer customer = customerService.updateCustomer(
                customerId,
                customerPersonalDetails);
        // record the onboarding step
        customerService.proceedToOnboardingStep(customerId, OnboardingStep.PERSONAL_DETAILS);
        CustomerDTO responseCustomerDTO = customerDTOToCustomerMapper.toCustomerDTO(customer);
        return ResponseEntity.ok(responseCustomerDTO);
    }

    @Override
    public ResponseEntity<CustomerDTO> addCustomerFinancials(UUID customerId, List<@Valid CustomerFinancialDTO> customerFinancialDTO) {
        List<CustomerFinancials> customerFinancialsList = customerFinancialDTO.stream()
                .map(customerDTOToCustomerMapper::toCustomerFinancials)
                .toList();
        // update customer financials
        Customer customer = customerService.updateCustomer(
                customerId,
                customerFinancialsList);
        // record the onboarding step
        customerService.proceedToOnboardingStep(customerId, OnboardingStep.FINANCIALS);
        CustomerDTO responseCustomerDTO = customerDTOToCustomerMapper.toCustomerDTO(customer);
        return ResponseEntity.ok(responseCustomerDTO);
    }

    @Override
    public ResponseEntity<CustomerDTO> addCustomerGoals(UUID customerId, List<@Valid CustomerGoalDTO> customerGoalDTO) {
        List<CustomerGoals> customerGoalsList = customerGoalDTO.stream()
                .map(customerDTOToCustomerMapper::toCustomerGoals)
                .toList();
        // update customer goals
        Customer customer = customerService.updateCustomer(
                customerId,
                customerGoalsList);
        // record the onboarding step
        customerService.proceedToOnboardingStep(customerId, OnboardingStep.GOALS);
        CustomerDTO responseCustomerDTO = customerDTOToCustomerMapper.toCustomerDTO(customer);
        return ResponseEntity.ok(responseCustomerDTO);
    }

    @Override
    public ResponseEntity<CustomerDTO> getCustomerById(UUID customerId) {
        Customer customer = customerService.getCustomerById(customerId);
        CustomerDTO responseCustomerDTO = customerDTOToCustomerMapper.toCustomerDTO(customer);
        return ResponseEntity.ok(responseCustomerDTO);
    }

    @Override
    public ResponseEntity<List<CustomerDTO>> getCustomersByUserId(UUID userId) {
        List<Customer> customers = customerService.getCustomersByUserId(userId);
        List<CustomerDTO> responseCustomerDTOs = customers.stream()
                .map(customerDTOToCustomerMapper::toCustomerDTO)
                .toList();
        return ResponseEntity.ok(responseCustomerDTOs);
    }
}
