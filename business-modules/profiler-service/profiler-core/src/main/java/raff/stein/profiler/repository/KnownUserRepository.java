package raff.stein.profiler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import raff.stein.profiler.model.entity.KnownUserEntity;

import java.util.UUID;

@Repository
public interface KnownUserRepository extends JpaRepository<KnownUserEntity, UUID> {

    /**
     * Atomic: user-created and customer-user-resolved arrive together on different threads, and a read-then-insert would
     * fail one of them on the primary key. Native SQL is fine here: known_users isn't tenant-scoped.
     */
    @Modifying
    @Query(value = """
            insert into known_users (user_id, email) values (:userId, :email)
            on conflict (user_id) do update set email = excluded.email
            """, nativeQuery = true)
    void upsert(@Param("userId") UUID userId, @Param("email") String email);
}
