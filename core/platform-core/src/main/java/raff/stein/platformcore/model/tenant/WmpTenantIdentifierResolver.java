package raff.stein.platformcore.model.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.util.StringUtils;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;

/**
 * Resolves the Hibernate tenant from the WMP context (JWT claim or CloudEvent extension).
 * Hibernate uses it to fill and filter the {@code @TenantId} column of {@link TenantScopedEntity}.
 */
public class WmpTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    /**
     * Returned when no tenant is in context: Hibernate needs a non-null id to open a session.
     * Reads of tenant-scoped data then match nothing, and writes are rejected by {@link TenantScopedEntity}.
     */
    public static final String NO_TENANT = "__none__";

    @Override
    public String resolveCurrentTenantIdentifier() {
        final WMPContext context = SecurityContextHolder.getContext();
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            return NO_TENANT;
        }
        return context.getTenantId();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
