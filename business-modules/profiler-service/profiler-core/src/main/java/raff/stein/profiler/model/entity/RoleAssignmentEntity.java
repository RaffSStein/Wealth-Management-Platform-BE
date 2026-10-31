package raff.stein.profiler.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

import java.util.UUID;

/**
 * A user holds a role in a bank branch (source of truth; user-service keeps a read-only copy for issuing tokens).
 * Global, not tenant-scoped: a PLATFORM_ADMIN works across banks, so every query filters on bankCode explicitly.
 */
@Entity
@Table(name = "role_assignments",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_assignment",
                columnNames = {"userId", "bankCode", "branchCode", "role"}),
        indexes = @Index(name = "idx_role_assignments_user", columnList = "userId"))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentEntity extends BaseDateEntity<Long> {

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

    /**
     * A {@link raff.stein.platformcore.security.role.WmpRole} name.
     */
    @Column(nullable = false, length = 30)
    private String role;

    /**
     * Email of whoever assigned it (the requester, for four-eyes assignments), or "bootstrap".
     */
    @Column(nullable = false)
    private String grantedBy;

    /**
     * Email of the second admin (four-eyes assignments only).
     */
    private String approvedBy;

    /**
     * The request this assignment comes from (four-eyes assignments only).
     */
    private Long requestId;
}
