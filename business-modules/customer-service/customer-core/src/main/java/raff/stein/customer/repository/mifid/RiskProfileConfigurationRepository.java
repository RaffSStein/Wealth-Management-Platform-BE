package raff.stein.customer.repository.mifid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.mifid.RiskProfileConfigurationEntity;

import java.util.List;

@Repository
public interface RiskProfileConfigurationRepository extends JpaRepository<RiskProfileConfigurationEntity, Long> {

    @Query("SELECT r FROM RiskProfileConfigurationEntity r WHERE r.questionnaire.id = :questionnaireId")
    List<RiskProfileConfigurationEntity> findByQuestionnaireId(@Param("questionnaireId") Long questionnaireId);
}
