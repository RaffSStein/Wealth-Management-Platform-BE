package raff.stein.platformcore.model.tenant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.TenantId;
import org.springframework.util.StringUtils;
import raff.stein.platformcore.exception.types.unauthorized.WmpContextException;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;

/**
 * Base class for business entities owned by a tenant (bank).
 * <p>
 * Hibernate fills {@code tenant_id} from {@link WmpTenantIdentifierResolver} on insert and adds a tenant predicate to
 * every JPQL/Criteria query, so a tenant never reads another tenant's rows. Native queries are NOT filtered.
 * </p>
 *
 * @param <I> the type of the entity identifier
 */
@MappedSuperclass
@NoArgsConstructor
@Getter
public abstract class TenantScopedEntity<I> extends BaseDateEntity<I> {

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64)
    @JsonIgnore
    private String tenantId;

    // without a tenant in context the row would be stored under the resolver's placeholder: refuse instead
    @PrePersist
    void requireTenantInContext() {
        final WMPContext context = SecurityContextHolder.getContext();
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw WmpContextException.forMissingField("tenantId").get();
        }
    }
}
