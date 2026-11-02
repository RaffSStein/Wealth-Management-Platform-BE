package raff.stein.platformcore.messaging.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    /**
     * Locks and returns the oldest events that are due. {@code -2} is "skip locked": rows another instance is already
     * delivering are left out, so instances never send the same event, and no scheduler lock is needed.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select e from OutboxEventEntity e where e.status = :status and e.nextAttemptAt <= :now order by e.id")
    List<OutboxEventEntity> findDue(@Param("status") OutboxStatus status, @Param("now") Instant now, Pageable pageable);
}
