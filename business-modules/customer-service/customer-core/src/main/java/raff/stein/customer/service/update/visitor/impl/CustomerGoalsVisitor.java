package raff.stein.customer.service.update.visitor.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.bo.customer.CustomerGoals;
import raff.stein.customer.model.entity.customer.CustomerFinancialGoalsEntity;
import raff.stein.customer.model.entity.customer.mapper.CustomerGoalsToCustomerGoalsEntityMapper;
import raff.stein.customer.model.entity.goals.GoalTypeEntity;
import raff.stein.customer.repository.goal.CustomerGoalsRepository;
import raff.stein.customer.repository.goal.GoalTypeRepository;
import raff.stein.customer.service.update.visitor.CustomerVisitor;
import raff.stein.customer.service.update.visitor.TypeKey;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CustomerGoalsVisitor implements CustomerVisitor<Collection<CustomerGoals>> {

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
        UUID customerId = customer.getId();
        // FE will send back items with an ID for update purposes
        // if the ID is null, it means it's a new goal to be created
        // some objects in the payload may already have an ID, so we need to distinguish between new and existing goals
        Map<Long, CustomerGoals> goalsFromFEToUpdateById = payload.stream()
                .filter(goal -> goal.getId() != null)
                .collect(Collectors.toMap(CustomerGoals::getId, Function.identity()));

        List<CustomerFinancialGoalsEntity> savedEntities;

        if (goalsFromFEToUpdateById.isEmpty()) {
            // no existing goals to update, just create new ones
            savedEntities = createNewGoals(payload, customerId);
        } else {
            // upsert cycle
            savedEntities = upsertGoals(payload, goalsFromFEToUpdateById, customerId);
        }
        // add to customer the new goals in order to return them to the FE
        customer.getCustomerGoals().addAll(
                savedEntities.stream()
                        .map(customerGoalsToCustomerGoalsEntityMapper::toCustomerGoals)
                        .toList());

        return customer;
    }

    private List<CustomerFinancialGoalsEntity> upsertGoals(
            @NonNull Collection<CustomerGoals> payload,
            Map<Long, CustomerGoals> goalsFromFEToUpdateById,
            UUID customerId) {
        // fetch existing goals for the customer
        Map<Long, CustomerFinancialGoalsEntity> customerGoalsEntityToUpdateById = customerGoalsRepository
                .findByIdIn(goalsFromFEToUpdateById.keySet())
                .stream()
                .collect(Collectors.toMap(CustomerFinancialGoalsEntity::getId, Function.identity()));
        // get goal types from the payload and map them by name and id
        // this is needed to ensure that we can set the goal type for new entities
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
        // update existing entities
        // we will update the existing entities with the data from the payload
        for(Map.Entry<Long, CustomerGoals> entry : goalsFromFEToUpdateById.entrySet()) {
            Long goalId = entry.getKey();
            CustomerGoals goalFromFE = entry.getValue();
            CustomerFinancialGoalsEntity existingEntity = customerGoalsEntityToUpdateById.get(goalId);
            if (existingEntity != null) {
                // Update existing entity
                existingEntity.updateFrom(customerGoalsToCustomerGoalsEntityMapper.toCustomerGoalsEntity(goalFromFE));
                entitiesToSave.add(existingEntity);
            }
        }
        List<CustomerFinancialGoalsEntity> savedEntities = customerGoalsRepository.saveAll(entitiesToSave);
        // explicit set goalType for new entities
        // it is required for the mapper later to work correctly
        savedEntities.forEach(
                e -> {
                    if(e.getGoalType() == null) {
                        e.setGoalType(goalTypeEntitiesById.getOrDefault(e.getGoalTypeId(), null));
                    }
                });
        return savedEntities;
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
