package raff.stein.profiler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.profiler.model.entity.KnownUserEntity;

import java.util.UUID;

@Repository
public interface KnownUserRepository extends JpaRepository<KnownUserEntity, UUID> {
}
