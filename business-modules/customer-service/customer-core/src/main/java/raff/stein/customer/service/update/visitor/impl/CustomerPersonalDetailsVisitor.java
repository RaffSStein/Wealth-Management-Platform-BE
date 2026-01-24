package raff.stein.customer.service.update.visitor.impl;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.bo.customer.CustomerPersonalDetails;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.mapper.CustomerToCustomerEntityMapper;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.service.update.visitor.CustomerVisitor;
import raff.stein.customer.service.update.visitor.TypeKey;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomerPersonalDetailsVisitor implements CustomerVisitor<CustomerPersonalDetails> {

    private final CustomerRepository customerRepository;

    private static final CustomerToCustomerEntityMapper customerToCustomerEntityMapper = CustomerToCustomerEntityMapper.MAPPER;

    @Override
    public TypeKey getPayloadType() {
        return TypeKey.of(CustomerPersonalDetails.class, null, null);
    }

    @Override
    public Customer visit(Customer customer, @NonNull CustomerPersonalDetails payload) {
        UUID customerId = customer.getId();

        CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));

        applyPersonalDetailsUpdate(customerEntity, payload);

        CustomerEntity savedCustomer = customerRepository.save(customerEntity);
        return customerToCustomerEntityMapper.toCustomer(savedCustomer);
    }

    private static void applyPersonalDetailsUpdate(CustomerEntity customerEntity, CustomerPersonalDetails payload) {
        if (payload.firstName() != null) {
            customerEntity.setFirstName(payload.firstName());
        }
        if (payload.lastName() != null) {
            customerEntity.setLastName(payload.lastName());
        }
        if (payload.dateOfBirth() != null) {
            customerEntity.setDateOfBirth(payload.dateOfBirth());
        }
        if (payload.gender() != null) {
            customerEntity.setGender(payload.gender());
        }
        if (payload.taxId() != null) {
            customerEntity.setTaxId(payload.taxId());
        }
        if (payload.nationality() != null) {
            customerEntity.setNationality(payload.nationality());
        }
        if (payload.phoneNumber() != null) {
            customerEntity.setPhoneNumber(payload.phoneNumber());
        }
        if (payload.country() != null) {
            customerEntity.setCountry(payload.country());
        }
        if (payload.addressLine1() != null) {
            customerEntity.setAddressLine1(payload.addressLine1());
        }
        if (payload.addressLine2() != null) {
            customerEntity.setAddressLine2(payload.addressLine2());
        }
        if (payload.city() != null) {
            customerEntity.setCity(payload.city());
        }
        if (payload.zipCode() != null) {
            customerEntity.setZipCode(payload.zipCode());
        }
    }
}
