package raff.stein.customer.repository.aml;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.aml.AmlVerificationEntity;

@Repository
public interface AmlVerificationRepository extends JpaRepository<AmlVerificationEntity, Long> {
}
