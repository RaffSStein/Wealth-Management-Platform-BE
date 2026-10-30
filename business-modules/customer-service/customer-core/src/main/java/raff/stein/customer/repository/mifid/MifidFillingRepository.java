package raff.stein.customer.repository.mifid;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MifidFillingRepository extends JpaRepository<MifidFillingEntity, Long> {

    /**
     * Finds the latest non-deprecated MifidFillingEntity for a given customer.
     * The latest is the one with the highest id: fillingDate is not unique and is reset by updates.
     */
    @EntityGraph(attributePaths = {
            "responses",
            "responses.question",
            "responses.answerOption"
    })
    Optional<MifidFillingEntity> findTopByCustomerIdAndStatusNotOrderByIdDesc(UUID customerId, MifidFillingStatus status);

    // latest filling in the given status, e.g. the current SUBMITTED one during a renewal
    @EntityGraph(attributePaths = {
            "responses",
            "responses.question",
            "responses.answerOption"
    })
    Optional<MifidFillingEntity> findTopByCustomerIdAndStatusOrderByIdDesc(UUID customerId, MifidFillingStatus status);

    List<MifidFillingEntity> findByCustomerIdAndStatusAndIdNot(UUID customerId, MifidFillingStatus status, Long id);

    // scoped to the customer so a filling of another customer can't be read or changed through this path
    @EntityGraph(attributePaths = {
            "responses",
            "responses.question",
            "responses.answerOption"
    })
    Optional<MifidFillingEntity> findByIdAndCustomerId(Long id, UUID customerId);

}
