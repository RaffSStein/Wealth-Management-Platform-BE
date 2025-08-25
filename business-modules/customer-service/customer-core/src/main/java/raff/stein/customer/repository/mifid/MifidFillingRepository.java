package raff.stein.customer.repository.mifid;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MifidFillingRepository extends JpaRepository<MifidFillingEntity, Long> {

    /**
     * Finds the latest non-deprecated MifidFillingEntity for a given customer.
     * The latest is determined by the most recent fillingDate.
     */
    @EntityGraph(attributePaths = {
            "responses",
            "responses.question",
            "responses.answerOption"
    })
    Optional<MifidFillingEntity> findTopByCustomerIdAndStatusNotOrderByFillingDateDesc(UUID customerId, MifidFillingStatus status);

}
