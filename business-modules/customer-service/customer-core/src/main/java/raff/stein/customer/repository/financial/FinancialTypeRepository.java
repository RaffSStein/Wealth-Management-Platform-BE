package raff.stein.customer.repository.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.financial.FinancialTypeEntity;

import java.util.List;

@Repository
public interface FinancialTypeRepository extends JpaRepository<FinancialTypeEntity, Integer> {

    List<FinancialTypeEntity> findAllByNameIn(List<String> names);
}
