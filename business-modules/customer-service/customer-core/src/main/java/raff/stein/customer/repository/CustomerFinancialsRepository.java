package raff.stein.customer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.customer.CustomerFinancialEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerFinancialsRepository extends JpaRepository<CustomerFinancialEntity, Long> {

    List<CustomerFinancialEntity> findByCustomerId(UUID customerId);
}
