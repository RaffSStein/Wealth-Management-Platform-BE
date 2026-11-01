package raff.stein.customer.repository.customer;

import lombok.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.bo.dashboard.CustomerStatusProfileCount;
import raff.stein.customer.model.entity.customer.CustomerEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID>, JpaSpecificationExecutor<CustomerEntity> {

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

    boolean existsByIdAndUserId(@NonNull UUID id, @NonNull UUID userId);

    boolean existsByUserId(@NonNull UUID userId);

    List<CustomerEntity> findByUserIdAndBranchCode(@NonNull UUID userId, @NonNull String branchCode);

    boolean existsByIdAndBranchCode(@NonNull UUID id, @NonNull String branchCode);

    Optional<CustomerEntity> findByEmail(@NonNull String email);

    boolean existsByEmail(@NonNull String email);

    @Query("""
            select new raff.stein.customer.model.bo.dashboard.CustomerStatusProfileCount(c.customerStatus, c.riskProfile, count(c))
            from CustomerEntity c
            where c.customerStatus <> raff.stein.customer.model.entity.customer.enumeration.CustomerStatus.DELETED
              and c.branchCode = :branchCode
            group by c.customerStatus, c.riskProfile
            """)
    List<CustomerStatusProfileCount> countByStatusAndRiskProfileInBranch(@Param("branchCode") String branchCode);

}
