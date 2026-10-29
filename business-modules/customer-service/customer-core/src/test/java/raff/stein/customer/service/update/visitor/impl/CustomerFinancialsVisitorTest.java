package raff.stein.customer.service.update.visitor.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.bo.customer.CustomerFinancials;
import raff.stein.customer.model.bo.financial.FinancialType;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.CustomerFinancialEntity;
import raff.stein.customer.model.entity.financial.FinancialTypeEntity;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.repository.financial.CustomerFinancialsRepository;
import raff.stein.customer.repository.financial.FinancialTypeRepository;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class CustomerFinancialsVisitorTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    private static final FinancialTypeEntity SALARY = FinancialTypeEntity.builder().id(1).name("SALARY").build();
    private static final FinancialTypeEntity SAVINGS = FinancialTypeEntity.builder().id(2).name("SAVINGS").build();

    private CustomerRepository customerRepository;
    private CustomerFinancialsRepository customerFinancialsRepository;
    private FinancialTypeRepository financialTypeRepository;
    private CustomerFinancialsVisitor visitor;

    private Set<CustomerFinancialEntity> storedFinancials;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        customerFinancialsRepository = mock(CustomerFinancialsRepository.class);
        financialTypeRepository = mock(FinancialTypeRepository.class);
        visitor = new CustomerFinancialsVisitor(customerRepository, customerFinancialsRepository, financialTypeRepository);

        storedFinancials = new HashSet<>(List.of(
                storedFinancial(10L, SALARY, "1000"),
                storedFinancial(11L, SAVINGS, "500")));
        final CustomerEntity customerEntity = CustomerEntity.builder()
                .id(CUSTOMER_ID)
                .customerFinancials(storedFinancials)
                .build();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customerEntity));
        when(financialTypeRepository.findAllByNameIn(anyList())).thenReturn(List.of(SALARY, SAVINGS));
        when(customerFinancialsRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldUpdateOwnedInsertNewAndDeleteMissingFinancials() {
        final List<CustomerFinancials> payload = List.of(
                financial(10L, "SALARY", "2000"),
                financial(null, "savings", "300"));

        final Customer result = visitor.visit(customer(), payload);

        // 11 is missing from the payload, so it is removed (orphan removal deletes it)
        assertEquals(1, storedFinancials.size());
        final CustomerFinancialEntity updated = storedFinancials.iterator().next();
        assertEquals(10L, updated.getId());
        assertEquals(new BigDecimal("2000"), updated.getAmount());

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<CustomerFinancialEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(customerFinancialsRepository).saveAll(captor.capture());
        final List<CustomerFinancialEntity> created = captor.getValue();
        assertEquals(1, created.size());
        assertEquals(CUSTOMER_ID, created.getFirst().getCustomerId());
        assertEquals(SAVINGS.getId(), created.getFirst().getFinancialTypeId());

        // the response contains the full resulting set
        assertEquals(2, result.getCustomerFinancials().size());
        assertEquals(10L, result.getCustomerFinancials().getFirst().getId());
    }

    @Test
    void shouldDeleteAllStoredFinancialsWhenPayloadHasOnlyNewRows() {
        final Customer result = visitor.visit(customer(), List.of(financial(null, "SALARY", "100")));

        assertTrue(storedFinancials.isEmpty());
        assertEquals(1, result.getCustomerFinancials().size());
    }

    @Test
    void shouldRejectFinancialIdNotOwnedByCustomer() {
        final List<CustomerFinancials> payload = List.of(
                financial(10L, "SALARY", "2000"),
                financial(99L, "SALARY", "1"));

        assertThrows(RequestValidationException.class, () -> visitor.visit(customer(), payload));

        assertEquals(2, storedFinancials.size());
        assertTrue(storedFinancials.stream().allMatch(e -> e.getAmount().compareTo(new BigDecimal("2000")) != 0));
        verify(customerFinancialsRepository, never()).saveAll(any());
    }

    @Test
    void shouldRejectDuplicatedFinancialIds() {
        final List<CustomerFinancials> payload = List.of(
                financial(10L, "SALARY", "2000"),
                financial(10L, "SALARY", "3000"));

        assertThrows(RequestValidationException.class, () -> visitor.visit(customer(), payload));

        assertEquals(2, storedFinancials.size());
        verify(customerFinancialsRepository, never()).saveAll(any());
    }

    private static Customer customer() {
        return Customer.builder().id(CUSTOMER_ID).customerFinancials(new ArrayList<>()).build();
    }

    private static CustomerFinancials financial(Long id, String typeName, String amount) {
        return CustomerFinancials.builder()
                .id(id)
                .financialType(FinancialType.builder().name(typeName).build())
                .amount(new BigDecimal(amount))
                .build();
    }

    private static CustomerFinancialEntity storedFinancial(Long id, FinancialTypeEntity type, String amount) {
        return CustomerFinancialEntity.builder()
                .id(id)
                .customerId(CUSTOMER_ID)
                .financialType(type)
                .financialTypeId(type.getId())
                .amount(new BigDecimal(amount))
                .build();
    }
}
