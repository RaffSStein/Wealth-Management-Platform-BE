package raff.stein.customer.repository.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingStepEntity;

import java.util.Optional;

@Repository
public interface CustomerOnboardingStepRepository extends JpaRepository<CustomerOnboardingStepEntity, Long> {

    Optional<CustomerOnboardingStepEntity> findByCustomerOnboardingAndStep(
            CustomerOnboardingEntity customerOnboarding,
            OnboardingStep step);


}
