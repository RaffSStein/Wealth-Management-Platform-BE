package raff.stein.customer.repository.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.customer.CustomerFinancialGoalsEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface CustomerGoalsRepository extends JpaRepository<CustomerFinancialGoalsEntity, Long> {

    List<CustomerFinancialGoalsEntity> findByCustomerId(UUID customerId);

    List<CustomerFinancialGoalsEntity> findByIdIn(Set<Long> ids);

}
