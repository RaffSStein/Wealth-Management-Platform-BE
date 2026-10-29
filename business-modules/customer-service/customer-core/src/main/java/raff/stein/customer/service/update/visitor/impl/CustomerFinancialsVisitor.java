package raff.stein.customer.service.update.visitor.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.customer.exception.CustomerNotFoundException;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.model.bo.customer.CustomerFinancials;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.CustomerFinancialEntity;
import raff.stein.customer.model.entity.customer.mapper.CustomerFinancialsToCustomerFinancialEntityMapper;
import raff.stein.customer.model.entity.financial.FinancialTypeEntity;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.repository.financial.CustomerFinancialsRepository;
import raff.stein.customer.repository.financial.FinancialTypeRepository;
import raff.stein.customer.service.update.visitor.CustomerVisitor;
import raff.stein.customer.service.update.visitor.TypeKey;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Replaces the customer's financials with the payload: rows with an id are updated, rows without an id are
 * created and the customer's rows missing from the payload are deleted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerFinancialsVisitor implements CustomerVisitor<Collection<CustomerFinancials>> {

    private final CustomerRepository customerRepository;
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
        final UUID customerId = customer.getId();

        final Map<Long, CustomerFinancials> financialsFromFEToUpdateById = payload.stream()
                .filter(financial -> financial.getId() != null)
                .collect(Collectors.toMap(
                        CustomerFinancials::getId,
                        Function.identity(),
                        (first, duplicate) -> {
                            throw invalidFinancialId(customerId, first.getId());
                        }));

        // already managed in the current transaction, so this does not query the DB again
        final CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(CustomerNotFoundException.with(customerId));
        final Set<CustomerFinancialEntity> existingFinancials = customerEntity.getCustomerFinancials();
        final Set<Long> ownedFinancialIds = existingFinancials.stream()
                .map(CustomerFinancialEntity::getId)
                .collect(Collectors.toSet());

        // ids must belong to this customer: unknown or foreign ids are rejected, never silently ignored
        financialsFromFEToUpdateById.keySet().stream()
                .filter(financialId -> !ownedFinancialIds.contains(financialId))
                .findFirst()
                .ifPresent(financialId -> {
                    throw invalidFinancialId(customerId, financialId);
                });

        // rows missing from the payload are deleted through orphan removal
        existingFinancials.removeIf(entity -> !financialsFromFEToUpdateById.containsKey(entity.getId()));
        existingFinancials.forEach(entity -> entity.updateFrom(
                customerFinancialsToCustomerFinancialEntityMapper.toCustomerFinancialsEntity(
                        financialsFromFEToUpdateById.get(entity.getId()))));

        final List<CustomerFinancialEntity> createdEntities = createNewFinancials(payload, customerId);

        // return the resulting full set to the FE
        customer.setCustomerFinancials(Stream.concat(existingFinancials.stream(), createdEntities.stream())
                .sorted(Comparator.comparing(CustomerFinancialEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(customerFinancialsToCustomerFinancialEntityMapper::toCustomerFinancials)
                .toList());
        return customer;
    }

    private static RequestValidationException invalidFinancialId(UUID customerId, Long financialId) {
        log.warn("Rejecting financial id: [{}] for customerId: [{}], duplicated or not owned by the customer", financialId, customerId);
        return new RequestValidationException(
                ErrorCode.REQUEST_VALIDATION_ERROR,
                String.format(ErrorCode.REQUEST_VALIDATION_ERROR.getMessageTemplate(), "financials.id", financialId));
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
