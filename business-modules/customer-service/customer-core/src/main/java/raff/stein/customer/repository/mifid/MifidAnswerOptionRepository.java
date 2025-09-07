package raff.stein.customer.repository.mifid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.mifid.MifidAnswerOptionEntity;

import java.util.List;

@Repository
public interface MifidAnswerOptionRepository extends JpaRepository<MifidAnswerOptionEntity, Long> {
    List<MifidAnswerOptionEntity> findByIdIn(List<Long> ids);

}
