package raff.stein.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.user.model.entity.BranchUserEntity;

import java.util.Optional;

@Repository
public interface BranchUserRepository extends JpaRepository<BranchUserEntity, Long> {

    Optional<BranchUserEntity> findByAssignmentId(Long assignmentId);
}
