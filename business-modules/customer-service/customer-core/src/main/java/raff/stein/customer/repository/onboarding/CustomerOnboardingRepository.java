package raff.stein.customer.repository.onboarding;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerOnboardingRepository extends JpaRepository<CustomerOnboardingEntity, Long> {

    @EntityGraph(attributePaths = "onboardingSteps")
    Optional<CustomerOnboardingEntity> findByCustomerIdAndIsValidTrue(UUID customerId);

    @EntityGraph(attributePaths = "onboardingSteps")
    Optional<CustomerOnboardingEntity> findByIdAndCustomerId(Long id, UUID customerId);

    @EntityGraph(attributePaths = "onboardingSteps")
    List<CustomerOnboardingEntity> findAllByCustomerId(UUID customerId);
}
