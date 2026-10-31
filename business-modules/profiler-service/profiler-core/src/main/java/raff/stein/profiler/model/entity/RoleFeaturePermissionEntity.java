package raff.stein.profiler.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import raff.stein.platformcore.model.audit.entity.BaseEntity;

/**
 * What a role may do: one permission code on one feature. A user's permissions are those of the role it holds in the
 * bank branch of its token, computed on read: nothing is copied per user. Catalogue data, like sections and features.
 */
@Entity
@Table(name = "role_feature_permission", uniqueConstraints = @UniqueConstraint(
        name = "uk_role_feature_permission", columnNames = {"role", "feature_id", "permission_id"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleFeaturePermissionEntity extends BaseEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * A {@link raff.stein.platformcore.security.role.WmpRole} name. Plain column: roles are an enum in code.
     */
    @Column(nullable = false, length = 30)
    private String role;

    @ManyToOne(optional = false)
    @JoinColumn(name = "feature_id")
    private FeatureEntity feature;

    @ManyToOne(optional = false)
    @JoinColumn(name = "permission_id")
    private PermissionEntity permission;
}
