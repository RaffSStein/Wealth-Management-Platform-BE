package raff.stein.profiler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.profiler.model.entity.PermissionEntity;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, Integer> {

    Optional<PermissionEntity> findByPermissionCode(String permissionCode);
}
