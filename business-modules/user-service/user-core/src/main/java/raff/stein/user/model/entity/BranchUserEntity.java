package raff.stein.user.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

/**
 * Read-only copy of a role assignment owned by profiler-service: the user holds a role in a bank branch.
 * Written only by the role-assignment event consumers; login and /me read it to issue the token. Never edit it here.
 */
@Entity
@Table(
        name = "branch_users",
        uniqueConstraints = {@UniqueConstraint(name = "uk_branch_user_role", columnNames = {"user_id", "bankCode", "branchCode", "role"})}
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchUserEntity extends BaseDateEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user associated with this bank branch role.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private UserEntity user;

    /**
     * The bank (tenant) of this assignment. It becomes the token's tenantId when the assignment is active.
     * Plain column, not @TenantId: login must read the assignments of every bank.
     */
    @Column(nullable = false)
    private String bankCode;

    /**
     * The bank branch where the user operates.
     */
    @Column(nullable = false)
    private String branchCode;

    /**
     * The role held in the bank branch: a {@link raff.stein.platformcore.security.role.WmpRole} name.
     */
    @Column(nullable = false, length = 30)
    private String role;

    /**
     * Additional notes or description for this association (optional).
     */
    @Column
    private String notes;

    /**
     * Id of the role assignment in profiler-service: makes event replays idempotent. Null only for rows created
     * before role assignments existed.
     */
    @Column(unique = true)
    private Long assignmentId;
}
