package raff.stein.customer.repository.customer;

import lombok.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.customer.CustomerEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    @EntityGraph(attributePaths = {
            "customerFinancialGoals",
            "customerFinancialGoals.goalType",
            "customerFinancials",
            "customerFinancials.financialType",
            "amlVerifications",
            "mifidFillings",
            "customerOnboardingStatuses"
    })
    Optional<CustomerEntity> findById(@NonNull UUID id);

    List<CustomerEntity> findByUserId(@NonNull UUID userId);

}
