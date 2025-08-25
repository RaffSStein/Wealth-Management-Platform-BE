package raff.stein.customer.repository.mifid;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.mifid.MifidQuestionnaireEntity;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MifidQuestionnaireRepository extends JpaRepository<MifidQuestionnaireEntity, Long> {

    @EntityGraph(attributePaths = {
            "riskProfiles",
            "sections",
            "sections.questions",
            "sections.questions.answerOptions"
    })
    @Query("SELECT q FROM MifidQuestionnaireEntity q WHERE :today BETWEEN q.validFrom AND q.validTo ORDER BY q.validFrom DESC")
    Optional<MifidQuestionnaireEntity> findValidQuestionnaire(@Param("today") LocalDate today);

    @Query("SELECT COUNT(q) FROM MifidQuestionEntity q WHERE q.section.questionnaire.id = :questionnaireId")
    long countQuestionsByQuestionnaireId(@Param("questionnaireId") Long questionnaireId);

    //TODO: handle customer fillings of old questionnaires (disable those fillings when a new questionnaire is created)
}
