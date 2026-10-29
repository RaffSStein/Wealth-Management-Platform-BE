package raff.stein.customer.service.update.visitor.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.bo.customer.CustomerGoals;
import raff.stein.customer.model.bo.goals.GoalType;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.CustomerFinancialGoalsEntity;
import raff.stein.customer.model.entity.goals.GoalTypeEntity;
import raff.stein.customer.model.entity.goals.enumeration.GoalTimeline;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.repository.goal.CustomerGoalsRepository;
import raff.stein.customer.repository.goal.GoalTypeRepository;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class CustomerGoalsVisitorTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    private static final GoalTypeEntity RETIREMENT = GoalTypeEntity.builder().id(1).name("RETIREMENT").build();
    private static final GoalTypeEntity HOUSE = GoalTypeEntity.builder().id(2).name("HOUSE").build();

    private CustomerRepository customerRepository;
    private CustomerGoalsRepository customerGoalsRepository;
    private GoalTypeRepository goalTypeRepository;
    private CustomerGoalsVisitor visitor;

    private Set<CustomerFinancialGoalsEntity> storedGoals;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        customerGoalsRepository = mock(CustomerGoalsRepository.class);
        goalTypeRepository = mock(GoalTypeRepository.class);
        visitor = new CustomerGoalsVisitor(customerRepository, customerGoalsRepository, goalTypeRepository);

        storedGoals = new HashSet<>(List.of(
                storedGoal(20L, RETIREMENT, "100000"),
                storedGoal(21L, HOUSE, "50000")));
        final CustomerEntity customerEntity = CustomerEntity.builder()
                .id(CUSTOMER_ID)
                .customerFinancialGoals(storedGoals)
                .build();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customerEntity));
        when(goalTypeRepository.findAllByNameIn(anyList())).thenReturn(List.of(RETIREMENT, HOUSE));
        when(customerGoalsRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldUpdateOwnedInsertNewAndDeleteMissingGoals() {
        final List<CustomerGoals> payload = List.of(
                goal(21L, "HOUSE", "80000"),
                goal(null, "retirement", "200000"));

        final Customer result = visitor.visit(customer(), payload);

        // 20 is missing from the payload, so it is removed (orphan removal deletes it)
        assertEquals(1, storedGoals.size());
        final CustomerFinancialGoalsEntity updated = storedGoals.iterator().next();
        assertEquals(21L, updated.getId());
        assertEquals(new BigDecimal("80000"), updated.getTargetAmount());

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<CustomerFinancialGoalsEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(customerGoalsRepository).saveAll(captor.capture());
        final List<CustomerFinancialGoalsEntity> created = captor.getValue();
        assertEquals(1, created.size());
        assertEquals(CUSTOMER_ID, created.getFirst().getCustomerId());
        assertEquals(RETIREMENT.getId(), created.getFirst().getGoalTypeId());

        // the response contains the full resulting set
        assertEquals(2, result.getCustomerGoals().size());
        assertEquals(21L, result.getCustomerGoals().getFirst().getId());
    }

    @Test
    void shouldRejectGoalIdNotOwnedByCustomer() {
        final List<CustomerGoals> payload = List.of(goal(99L, "HOUSE", "1"));

        assertThrows(RequestValidationException.class, () -> visitor.visit(customer(), payload));

        assertEquals(2, storedGoals.size());
        verify(customerGoalsRepository, never()).saveAll(any());
    }

    @Test
    void shouldRejectDuplicatedGoalIds() {
        final List<CustomerGoals> payload = List.of(
                goal(20L, "RETIREMENT", "1"),
                goal(20L, "RETIREMENT", "2"));

        assertThrows(RequestValidationException.class, () -> visitor.visit(customer(), payload));

        assertEquals(2, storedGoals.size());
        verify(customerGoalsRepository, never()).saveAll(any());
    }

    private static Customer customer() {
        return Customer.builder().id(CUSTOMER_ID).customerGoals(new ArrayList<>()).build();
    }

    private static CustomerGoals goal(Long id, String typeName, String targetAmount) {
        return CustomerGoals.builder()
                .id(id)
                .goalType(GoalType.builder().name(typeName).build())
                .targetAmount(new BigDecimal(targetAmount))
                .goalTimeline(GoalTimeline.LONG_TERM)
                .build();
    }

    private static CustomerFinancialGoalsEntity storedGoal(Long id, GoalTypeEntity type, String targetAmount) {
        return CustomerFinancialGoalsEntity.builder()
                .id(id)
                .customerId(CUSTOMER_ID)
                .goalType(type)
                .goalTypeId(type.getId())
                .goalTimeline(GoalTimeline.MEDIUM_TERM)
                .targetAmount(new BigDecimal(targetAmount))
                .build();
    }
}
