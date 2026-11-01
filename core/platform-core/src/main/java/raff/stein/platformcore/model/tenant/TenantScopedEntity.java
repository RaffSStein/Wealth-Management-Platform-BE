package raff.stein.platformcore.model.tenant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.TenantId;
import org.springframework.util.StringUtils;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;
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
        refuseWriteAsPlatformAdmin();
    }

    // a platform admin has no bank of its own: its rows would land under the pseudo tenant, or in the wrong bank as root
    @PreUpdate
    @PreRemove
    void refuseWriteAsPlatformAdmin() {
        final WMPContext context = SecurityContextHolder.getContext();
        if (WmpTenantIdentifierResolver.isPlatformAdmin(context)) {
            throw new AccessDeniedException(
                    ErrorCode.ACCESS_DENIED,
                    String.format(ErrorCode.ACCESS_DENIED.getMessageTemplate(), context.getUserId(), getId()));
        }
    }
}
