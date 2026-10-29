package raff.stein.customer.service.update.visitor.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.customer.exception.CustomerNotFoundException;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.bo.customer.CustomerGoals;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.CustomerFinancialGoalsEntity;
import raff.stein.customer.model.entity.customer.mapper.CustomerGoalsToCustomerGoalsEntityMapper;
import raff.stein.customer.model.entity.goals.GoalTypeEntity;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.repository.goal.CustomerGoalsRepository;
import raff.stein.customer.repository.goal.GoalTypeRepository;
import raff.stein.customer.service.update.visitor.CustomerVisitor;
import raff.stein.customer.service.update.visitor.TypeKey;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Replaces the customer's goals with the payload: rows with an id are updated, rows without an id are
 * created and the customer's rows missing from the payload are deleted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerGoalsVisitor implements CustomerVisitor<Collection<CustomerGoals>> {

    private final CustomerRepository customerRepository;
    private final CustomerGoalsRepository customerGoalsRepository;
    private final GoalTypeRepository goalTypeRepository;

    private static final CustomerGoalsToCustomerGoalsEntityMapper customerGoalsToCustomerGoalsEntityMapper =
            CustomerGoalsToCustomerGoalsEntityMapper.MAPPER;

    @Override
    public TypeKey getPayloadType() {
        return TypeKey.of(Collection.class, CustomerGoals.class, null);
    }

    @Override
    public Customer visit(Customer customer, @NonNull Collection<CustomerGoals> payload) {
        final UUID customerId = customer.getId();

        final Map<Long, CustomerGoals> goalsFromFEToUpdateById = payload.stream()
                .filter(goal -> goal.getId() != null)
                .collect(Collectors.toMap(
                        CustomerGoals::getId,
                        Function.identity(),
                        (first, duplicate) -> {
                            throw invalidGoalId(customerId, first.getId());
                        }));

        // already managed in the current transaction, so this does not query the DB again
        final CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(CustomerNotFoundException.with(customerId));
        final Set<CustomerFinancialGoalsEntity> existingGoals = customerEntity.getCustomerFinancialGoals();
        final Set<Long> ownedGoalIds = existingGoals.stream()
                .map(CustomerFinancialGoalsEntity::getId)
                .collect(Collectors.toSet());

        // ids must belong to this customer: unknown or foreign ids are rejected, never silently ignored
        goalsFromFEToUpdateById.keySet().stream()
                .filter(goalId -> !ownedGoalIds.contains(goalId))
                .findFirst()
                .ifPresent(goalId -> {
                    throw invalidGoalId(customerId, goalId);
                });

        // rows missing from the payload are deleted through orphan removal
        existingGoals.removeIf(entity -> !goalsFromFEToUpdateById.containsKey(entity.getId()));
        existingGoals.forEach(entity -> entity.updateFrom(
                customerGoalsToCustomerGoalsEntityMapper.toCustomerGoalsEntity(
                        goalsFromFEToUpdateById.get(entity.getId()))));

        final List<CustomerFinancialGoalsEntity> createdEntities = createNewGoals(payload, customerId);

        // return the resulting full set to the FE
        customer.setCustomerGoals(Stream.concat(existingGoals.stream(), createdEntities.stream())
                .sorted(Comparator.comparing(CustomerFinancialGoalsEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(customerGoalsToCustomerGoalsEntityMapper::toCustomerGoals)
                .toList());
        return customer;
    }

    private static RequestValidationException invalidGoalId(UUID customerId, Long goalId) {
        log.warn("Rejecting goal id: [{}] for customerId: [{}], duplicated or not owned by the customer", goalId, customerId);
        return new RequestValidationException(
                ErrorCode.REQUEST_VALIDATION_ERROR,
                String.format(ErrorCode.REQUEST_VALIDATION_ERROR.getMessageTemplate(), "goals.id", goalId));
    }

    private List<CustomerFinancialGoalsEntity> createNewGoals(
            @NonNull Collection<CustomerGoals> payload,
            UUID customerId) {
        // get goal types from the payload and map them by name and id
        List<GoalTypeEntity> goalTypes = getGoalTypeEntities(payload);

        Map<String, GoalTypeEntity> goalTypeEntitiesByName = goalTypes
                .stream()
                .collect(Collectors.toMap(GoalTypeEntity::getName, Function.identity()));

        Map<Integer, GoalTypeEntity> goalTypeEntitiesById = goalTypes
                .stream()
                .collect(Collectors.toMap(GoalTypeEntity::getId, Function.identity()));
        // get new entities from the payload
        List<CustomerFinancialGoalsEntity> newEntities = getNewEntities(payload, goalTypeEntitiesByName, customerId);

        List<CustomerFinancialGoalsEntity> entitiesToSave = new ArrayList<>(newEntities);
        List<CustomerFinancialGoalsEntity> savedEntities = customerGoalsRepository.saveAll(entitiesToSave);
        // explicit set goalType for new entities
        savedEntities.forEach(
                e -> {
                    if(e.getGoalType() == null) {
                        // Set goalType only for mapping purposes (Hibernate will not persist this due to updatable = false)
                        e.setGoalType(goalTypeEntitiesById.getOrDefault(e.getGoalTypeId(), null));
                    }
                });
        return savedEntities;

    }

    private static List<CustomerFinancialGoalsEntity> getNewEntities(
            Collection<CustomerGoals> payload,
            Map<String, GoalTypeEntity> goalTypeEntitiesByName,
            UUID customerId) {

        return payload.stream()
                .filter(goal -> goal.getId() == null) // Only new goals
                .filter(customerGoal -> customerGoal.getGoalType() != null && customerGoal.getGoalType().getName() != null)
                .map(customerGoal -> {
                    CustomerFinancialGoalsEntity entity = customerGoalsToCustomerGoalsEntityMapper.toCustomerGoalsEntity(customerGoal);
                    GoalTypeEntity goalType = goalTypeEntitiesByName.getOrDefault(
                            customerGoal.getGoalType().getName().toUpperCase(),
                            null);
                    if(goalType == null) {
                        throw new IllegalArgumentException("Goal type not found: " + customerGoal.getGoalType().getName());
                    }
                    entity.setCustomerId(customerId);
                    entity.setGoalTypeId(goalType.getId());
                    return entity;
                })
                .toList();
    }

    private List<GoalTypeEntity> getGoalTypeEntities(Collection<CustomerGoals> payload) {
        // fetch goal types from the repository
        List<String> goalTypeNames = payload.stream()
                .map(CustomerGoals::getGoalType)
                .filter(type -> type != null && type.getName() != null && !type.getName().isEmpty())
                .map(type -> type.getName().toUpperCase())
                .distinct()
                .toList();
        return goalTypeRepository.findAllByNameIn(goalTypeNames);
    }
}
