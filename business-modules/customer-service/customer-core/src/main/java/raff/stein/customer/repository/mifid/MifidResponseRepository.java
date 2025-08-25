package raff.stein.customer.repository.mifid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.mifid.MifidResponseEntity;

@Repository
public interface MifidResponseRepository extends JpaRepository<MifidResponseEntity, Long> {
}
