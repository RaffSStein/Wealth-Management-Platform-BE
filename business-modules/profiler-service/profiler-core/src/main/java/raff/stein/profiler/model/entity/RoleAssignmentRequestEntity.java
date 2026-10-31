package raff.stein.profiler.model.entity;

import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;
import raff.stein.profiler.model.entity.enumeration.RoleAssignmentRequestStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * A staff role assignment waiting for four-eyes approval. Kept after the decision as the audit trail of who asked and
 * who decided. Not tenant-scoped (PLATFORM_ADMIN works across banks): queries filter on bankCode explicitly.
 */
@Entity
@Table(name = "role_assignment_requests", indexes = {
        @Index(name = "idx_role_assignment_requests_bank_status", columnList = "bankCode, status")})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentRequestEntity extends BaseDateEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String bankCode;

    @Column(nullable = false)
    private String branchCode;

    @Column(nullable = false, length = 30)
    private String role;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleAssignmentRequestStatus status;

    @Column(nullable = false)
    private String requestedBy;

    // compared by the "not the same person" checks: emails can change, user ids can't
    @Column(nullable = false)
    private String requestedByUserId;

    @Column(nullable = false)
    private Instant requestedAt;

    @Setter
    private String decidedBy;

    @Setter
    private Instant decidedAt;

    @Setter
    @Column(length = 500)
    private String reason;
}
