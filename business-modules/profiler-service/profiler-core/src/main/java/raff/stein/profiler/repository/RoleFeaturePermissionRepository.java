package raff.stein.profiler.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.profiler.model.entity.RoleFeaturePermissionEntity;

import java.util.Collection;
import java.util.List;

@Repository
public interface RoleFeaturePermissionRepository extends JpaRepository<RoleFeaturePermissionEntity, Integer> {

    // one query: feature, its section and the permission are needed to build the response
    @EntityGraph(attributePaths = {"feature", "feature.section", "permission"})
    List<RoleFeaturePermissionEntity> findAllByRoleIn(Collection<String> roles);
}
