package raff.stein.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.user.model.entity.BranchUserEntity;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.repository.BranchUserRepository;
import raff.stein.user.repository.UserRepository;

import java.util.UUID;

/**
 * Keeps branch_users in step with profiler-service's role assignments. Idempotent by assignment id, so replayed or
 * duplicated events don't create or remove anything twice.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleAssignmentReadModelService {

    private final BranchUserRepository branchUserRepository;
    private final UserRepository userRepository;

    @Transactional
    public void applyGranted(Long assignmentId, UUID userId, String bankCode, String branchCode, String role) {
        if (branchUserRepository.findByAssignmentId(assignmentId).isPresent()) {
            log.info("Role assignment [{}] already applied, skipping", assignmentId);
            return;
        }
        final UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Role assignment [{}] for unknown userId [{}], skipping", assignmentId, userId);
            return;
        }
        // a row with the same (user, bank, branch, role) but no assignment id predates role assignments: adopt it
        final BranchUserEntity existing = user.getBankBranchUsers().stream()
                .filter(m -> bankCode.equals(m.getBankCode()) && branchCode.equals(m.getBranchCode()) && role.equals(m.getRole()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            existing.setAssignmentId(assignmentId);
            log.info("Role assignment [{}] linked to existing branch user [{}]", assignmentId, existing.getId());
            return;
        }
        user.getBankBranchUsers().add(BranchUserEntity.builder()
                .user(user)
                .bankCode(bankCode)
                .branchCode(branchCode)
                .role(role)
                .assignmentId(assignmentId)
                .build());
        userRepository.save(user);
        log.info("Role assignment [{}] applied: role [{}] in [{}/{}] for userId [{}]",
                assignmentId, role, bankCode, branchCode, userId);
    }

    @Transactional
    public void applyRevoked(Long assignmentId) {
        branchUserRepository.findByAssignmentId(assignmentId).ifPresentOrElse(
                branchUser -> {
                    // orphanRemoval on UserEntity.bankBranchUsers deletes the row
                    branchUser.getUser().getBankBranchUsers().remove(branchUser);
                    log.info("Role assignment [{}] removed", assignmentId);
                },
                () -> log.info("Role assignment [{}] not present, nothing to remove", assignmentId));
    }
}
