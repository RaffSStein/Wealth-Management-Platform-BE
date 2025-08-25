package raff.stein.customer.service.update.visitor.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.bo.customer.CustomerFinancials;
import raff.stein.customer.model.entity.customer.CustomerFinancialEntity;
import raff.stein.customer.model.entity.customer.mapper.CustomerFinancialsToCustomerFinancialEntityMapper;
import raff.stein.customer.model.entity.financial.FinancialTypeEntity;
import raff.stein.customer.repository.financial.CustomerFinancialsRepository;
import raff.stein.customer.repository.financial.FinancialTypeRepository;
import raff.stein.customer.service.update.visitor.CustomerVisitor;
import raff.stein.customer.service.update.visitor.TypeKey;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CustomerFinancialsVisitor implements CustomerVisitor<Collection<CustomerFinancials>> {

    private final CustomerFinancialsRepository customerFinancialsRepository;
    private final FinancialTypeRepository financialTypeRepository;

    private static final CustomerFinancialsToCustomerFinancialEntityMapper customerFinancialsToCustomerFinancialEntityMapper =
            CustomerFinancialsToCustomerFinancialEntityMapper.MAPPER;

    @Override
    public TypeKey getPayloadType() {
        return TypeKey.of(Collection.class, CustomerFinancials.class, null);
    }

    @Override
    public Customer visit(Customer customer, @NonNull Collection<CustomerFinancials> payload) {
        UUID customerId = customer.getId();

        Map<Long, CustomerFinancials> financialsFromFEToUpdateById = payload.stream()
                .filter(financial -> financial.getId() != null)
                .collect(Collectors.toMap(CustomerFinancials::getId, Function.identity()));

        List<CustomerFinancialEntity> savedEntities;

        if (financialsFromFEToUpdateById.isEmpty()) {
            // no existing financials to update, just create new ones
            savedEntities = createNewFinancials(payload, customerId);
        } else {
            // upsert cycle
            savedEntities = upsertFinancials(payload, financialsFromFEToUpdateById, customerId);
        }

        // add to customer the new financials in order to return them to the FE
        customer.getCustomerFinancials().addAll(
                savedEntities.stream()
                        .map(customerFinancialsToCustomerFinancialEntityMapper::toCustomerFinancials)
                        .toList());
        return customer;
    }

    private List<CustomerFinancialEntity> upsertFinancials(
            @NonNull Collection<CustomerFinancials> payload,
            Map<Long, CustomerFinancials> financialsFromFEToUpdateById,
            UUID customerId) {

        // fetch existing financials for the customer
        Map<Long, CustomerFinancialEntity> customerFinancialEntityToUpdateById = customerFinancialsRepository
                .findByIdIn(financialsFromFEToUpdateById.keySet())
                .stream()
                .collect(Collectors.toMap(CustomerFinancialEntity::getId, Function.identity()));
        // get financial types from the payload and map them by name and id
        // this is needed to ensure that we can set the financial type for new entities
        List<FinancialTypeEntity> goalTypes = getFinancialTypeEntities(payload);
        Map<String, FinancialTypeEntity> financialTypeEntitiesByName = goalTypes
                .stream()
                .collect(Collectors.toMap(FinancialTypeEntity::getName, Function.identity()));
        Map<Integer, FinancialTypeEntity> financialTypeEntitiesById = goalTypes
                .stream()
                .collect(Collectors.toMap(FinancialTypeEntity::getId, Function.identity()));
        // get new entities from the payload
        List<CustomerFinancialEntity> newEntities = getNewEntities(payload, customerId, financialTypeEntitiesByName);

        List<CustomerFinancialEntity> entitiesToSave = new ArrayList<>(newEntities);
        // update existing entities
        // we will update the existing entities with the data from the payload
        for(Map.Entry<Long, CustomerFinancials> entry : financialsFromFEToUpdateById.entrySet()) {
            Long financialId = entry.getKey();
            CustomerFinancials financialFromFE = entry.getValue();
            CustomerFinancialEntity existingEntity = customerFinancialEntityToUpdateById.get(financialId);
            if (existingEntity != null) {
                // Update existing entity
                existingEntity.updateFrom(customerFinancialsToCustomerFinancialEntityMapper.toCustomerFinancialsEntity(financialFromFE));
                entitiesToSave.add(existingEntity);
            }
        }
        List<CustomerFinancialEntity> savedEntities = customerFinancialsRepository.saveAll(entitiesToSave);
        // explicit set goalType for new entities
        // it is required for the mapper later to work correctly
        savedEntities.forEach(
                e -> {
                    if(e.getFinancialType() == null) {
                        e.setFinancialType(financialTypeEntitiesById.getOrDefault(e.getFinancialTypeId(), null));
                    }
                });
        return savedEntities;
    }

    private List<CustomerFinancialEntity> createNewFinancials(
            @NonNull Collection<CustomerFinancials> payload,
            UUID customerId) {

        List<FinancialTypeEntity> financialTypeEntities = getFinancialTypeEntities(payload);

        Map<String, FinancialTypeEntity> financialTypeEntitiesByName = financialTypeEntities
                .stream()
                .collect(Collectors.toMap(FinancialTypeEntity::getName, Function.identity()));

        Map<Integer, FinancialTypeEntity> financialTypeEntitiesById = financialTypeEntities
                .stream()
                .collect(Collectors.toMap(FinancialTypeEntity::getId, Function.identity()));

        List<CustomerFinancialEntity> newByTypeId = getNewEntities(
                payload,
                customerId,
                financialTypeEntitiesByName);

        List<CustomerFinancialEntity> entitiesToSave = new ArrayList<>(newByTypeId);
        List<CustomerFinancialEntity> savedEntities = customerFinancialsRepository.saveAll(entitiesToSave);
        // explicit set financialType for new entities
        savedEntities.forEach(
                e -> {
                    if (e.getFinancialType() == null) {
                        // Set financialType only for mapping purposes (Hibernate will not persist this due to updatable = false)
                        e.setFinancialType(financialTypeEntitiesById.getOrDefault(e.getFinancialTypeId(), null));
                    }
                });
        return savedEntities;
    }

    private static List<CustomerFinancialEntity> getNewEntities(
            Collection<CustomerFinancials> payload,
            UUID customerId,
            Map<String, FinancialTypeEntity> financialTypeEntitiesByName) {

        return payload
                .stream()
                .filter(customerFinancials -> customerFinancials.getId() == null) // Only new financials
                .filter(customerFinancials -> customerFinancials.getFinancialType() != null &&
                        customerFinancials.getFinancialType().getName() != null)
                .map(customerFinancials -> {
                    CustomerFinancialEntity entity = customerFinancialsToCustomerFinancialEntityMapper.toCustomerFinancialsEntity(customerFinancials);
                    FinancialTypeEntity financialType = financialTypeEntitiesByName.getOrDefault(
                            customerFinancials.getFinancialType().getName().toUpperCase(),
                            null);
                    if (financialType == null) {
                        throw new IllegalArgumentException("Financial type not found: " +
                                customerFinancials.getFinancialType().getName());
                    }
                    entity.setFinancialTypeId(financialType.getId());
                    entity.setCustomerId(customerId);
                    return entity;
                })
                .toList();
    }

    private List<FinancialTypeEntity> getFinancialTypeEntities(Collection<CustomerFinancials> payload) {
        List<String> financialTypeNames = payload.stream()
                .map(CustomerFinancials::getFinancialType)
                .filter(type -> type != null && type.getName() != null && !type.getName().isEmpty())
                .map(type -> type.getName().toUpperCase())
                .distinct()
                .toList();
        return financialTypeRepository.findAllByNameIn(financialTypeNames);
    }


}
