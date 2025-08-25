package raff.stein.customer.repository.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.customer.CustomerOnboardingEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerOnboardingRepository extends JpaRepository<CustomerOnboardingEntity, Long> {

    Optional<CustomerOnboardingEntity> findByCustomerIdAndIsValidTrue(UUID customerId);
}
