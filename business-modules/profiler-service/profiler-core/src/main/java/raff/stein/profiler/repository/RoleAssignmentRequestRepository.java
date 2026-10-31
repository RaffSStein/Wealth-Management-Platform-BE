package raff.stein.profiler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raff.stein.profiler.model.entity.RoleAssignmentRequestEntity;
import raff.stein.profiler.model.entity.enumeration.RoleAssignmentRequestStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoleAssignmentRequestRepository extends JpaRepository<RoleAssignmentRequestEntity, Long> {

    boolean existsByUserIdAndBankCodeAndBranchCodeAndRoleAndStatus(
            UUID userId, String bankCode, String branchCode, String role, RoleAssignmentRequestStatus status);

    List<RoleAssignmentRequestEntity> findByBankCodeOrderByRequestedAtDesc(String bankCode);

    List<RoleAssignmentRequestEntity> findByBankCodeAndStatusOrderByRequestedAtDesc(
            String bankCode, RoleAssignmentRequestStatus status);

    List<RoleAssignmentRequestEntity> findAllByOrderByRequestedAtDesc();

    List<RoleAssignmentRequestEntity> findByStatusOrderByRequestedAtDesc(RoleAssignmentRequestStatus status);
}
