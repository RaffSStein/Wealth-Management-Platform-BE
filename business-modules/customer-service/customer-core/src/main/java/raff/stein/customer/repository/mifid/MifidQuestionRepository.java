package raff.stein.customer.repository.mifid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.mifid.MifidQuestionEntity;

import java.util.List;

@Repository
public interface MifidQuestionRepository extends JpaRepository<MifidQuestionEntity, Long> {

    List<MifidQuestionEntity> findByIdIn(List<Long> ids);

}
