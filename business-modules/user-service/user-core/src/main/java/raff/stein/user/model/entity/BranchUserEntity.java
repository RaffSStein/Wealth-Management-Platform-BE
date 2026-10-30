package raff.stein.user.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

/**
 * Entity representing the association between a user and a bank branch, including the user's role within that branch.
 * Each user can operate for multiple bank branches, with a specific role for each association.
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
     * The bank (tenant) of this membership. It becomes the token's tenantId when the membership is active.
     * Plain column, not @TenantId: login must read the memberships of every bank.
     */
    @Column(nullable = false)
    private String bankCode;

    /**
     * The bank branch where the user operates.
     */
    @Column(nullable = false)
    private String branchCode;

    /**
     * The role of the user within the bank branch (e.g., SUPERVISOR, HANDLER, MANAGER, AUDITOR, etc.).
     */
    @Column(nullable = false, length = 30)
    private String role;

    /**
     * Additional notes or description for this association (optional).
     */
    @Column
    private String notes;
}
