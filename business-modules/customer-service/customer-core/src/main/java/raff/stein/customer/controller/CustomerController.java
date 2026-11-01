package raff.stein.customer.controller;

import lombok.RequiredArgsConstructor;
import io.openapiprocessor.openapi.api.CustomerApi;
import io.openapiprocessor.openapi.model.AdvisorDashboardStatisticsDto;
import io.openapiprocessor.openapi.model.CustomerDto;
import io.openapiprocessor.openapi.model.CustomerStatus;
import io.openapiprocessor.openapi.model.CustomerSummaryDto;
import io.openapiprocessor.openapi.model.CustomerFinancialDto;
import io.openapiprocessor.openapi.model.CustomerGoalDto;
import io.openapiprocessor.openapi.model.CustomerPersonalDetailsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import raff.stein.customer.controller.mapper.customer.CustomerDTOToCustomerMapper;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.bo.customer.CustomerFinancials;
import raff.stein.customer.model.bo.customer.CustomerGoals;
import raff.stein.customer.model.bo.customer.CustomerPersonalDetails;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.service.CustomerService;
import raff.stein.customer.service.dashboard.AdvisorDashboardService;
import raff.stein.customer.controller.access.CustomerAccess;
import raff.stein.customer.controller.access.CustomerAccessLevel;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CustomerController implements CustomerApi {

    private final CustomerService customerService;
    private final AdvisorDashboardService advisorDashboardService;

    private static final CustomerDTOToCustomerMapper customerDTOToCustomerMapper = CustomerDTOToCustomerMapper.MAPPER;

    @Override
    public ResponseEntity<CustomerDto> initCustomer(CustomerDto customerDTO) {
        Customer customerInput = customerDTOToCustomerMapper.toCustomer(customerDTO);
        Customer createdCustomer = customerService.initCustomer(customerInput);
        CustomerDto responseCustomerDTO = customerDTOToCustomerMapper.toCustomerDTO(createdCustomer);
        return ResponseEntity.ok(responseCustomerDTO);
    }

    @CustomerAccess(CustomerAccessLevel.WRITE)
    @Override
    public ResponseEntity<CustomerDto> updateCustomerPersonalDetails(UUID customerId, CustomerPersonalDetailsDto customerPersonalDetailsDTO) {
        CustomerPersonalDetails customerPersonalDetails = customerDTOToCustomerMapper.toCustomerPersonalDetails(customerPersonalDetailsDTO);
        // update customer personal details
        Customer customer = customerService.updateCustomer(
                customerId,
                customerPersonalDetails);
        // record the onboarding step
        customerService.proceedToOnboardingStep(customerId, OnboardingStep.PERSONAL_DETAILS);
        CustomerDto responseCustomerDTO = customerDTOToCustomerMapper.toCustomerDTO(customer);
        return ResponseEntity.ok(responseCustomerDTO);
    }

    @CustomerAccess(CustomerAccessLevel.WRITE)
    @Override
    public ResponseEntity<CustomerDto> addCustomerFinancials(UUID customerId, List<CustomerFinancialDto> customerFinancialDTO) {
        List<CustomerFinancials> customerFinancialsList = customerFinancialDTO.stream()
                .map(customerDTOToCustomerMapper::toCustomerFinancials)
                .toList();
        // update customer financials
        Customer customer = customerService.updateCustomer(
                customerId,
                customerFinancialsList);
        // record the onboarding step
        customerService.proceedToOnboardingStep(customerId, OnboardingStep.FINANCIALS);
        CustomerDto responseCustomerDTO = customerDTOToCustomerMapper.toCustomerDTO(customer);
        return ResponseEntity.ok(responseCustomerDTO);
    }

    @CustomerAccess(CustomerAccessLevel.WRITE)
    @Override
    public ResponseEntity<CustomerDto> addCustomerGoals(UUID customerId, List<CustomerGoalDto> customerGoalDTO) {
        List<CustomerGoals> customerGoalsList = customerGoalDTO.stream()
                .map(customerDTOToCustomerMapper::toCustomerGoals)
                .toList();
        // update customer goals
        Customer customer = customerService.updateCustomer(
                customerId,
                customerGoalsList);
        // record the onboarding step
        customerService.proceedToOnboardingStep(customerId, OnboardingStep.GOALS);
        CustomerDto responseCustomerDTO = customerDTOToCustomerMapper.toCustomerDTO(customer);
        return ResponseEntity.ok(responseCustomerDTO);
    }

    @CustomerAccess(CustomerAccessLevel.READ)
    @Override
    public ResponseEntity<CustomerDto> getCustomerById(UUID customerId) {
        Customer customer = customerService.getCustomerById(customerId);
        CustomerDto responseCustomerDTO = customerDTOToCustomerMapper.toCustomerDTO(customer);
        return ResponseEntity.ok(responseCustomerDTO);
    }

    @CustomerAccess(CustomerAccessLevel.LIST)
    @Override
    public ResponseEntity<Page<CustomerSummaryDto>> getCustomers(String query, List<CustomerStatus> status, Pageable pageable) {
        final List<String> statuses = status == null ? List.of() : status.stream().map(CustomerStatus::name).toList();
        return ResponseEntity.ok(customerService.findCustomers(query, statuses, pageable)
                .map(customerDTOToCustomerMapper::toCustomerSummaryDTO));
    }

    @CustomerAccess(CustomerAccessLevel.ADVISOR_DASHBOARD)
    @Override
    public ResponseEntity<AdvisorDashboardStatisticsDto> getAdvisorDashboardStatistics() {
        return ResponseEntity.ok(customerDTOToCustomerMapper.toAdvisorDashboardStatisticsDto(
                advisorDashboardService.getStatistics()));
    }

    @CustomerAccess(CustomerAccessLevel.LIST_BY_USER)
    @Override
    public ResponseEntity<List<CustomerDto>> getCustomersByUserId(UUID userId) {
        List<Customer> customers = customerService.getCustomersByUserId(userId);
        List<CustomerDto> responseCustomerDTOs = customers.stream()
                .map(customerDTOToCustomerMapper::toCustomerDTO)
                .toList();
        return ResponseEntity.ok(responseCustomerDTOs);
    }
}
