package raff.stein.profiler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.profiler.model.entity.RoleAssignmentEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoleAssignmentRepository extends JpaRepository<RoleAssignmentEntity, Long> {

    boolean existsByUserIdAndBankCodeAndBranchCodeAndRole(UUID userId, String bankCode, String branchCode, String role);

    boolean existsByUserEmailAndRole(String userEmail, String role);

    List<RoleAssignmentEntity> findByUserIdOrderByIdAsc(UUID userId);

    List<RoleAssignmentEntity> findByUserIdAndBankCodeAndBranchCode(UUID userId, String bankCode, String branchCode);
}
