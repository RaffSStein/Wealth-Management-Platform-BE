package raff.stein.platformcore.model.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.util.StringUtils;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.role.WmpRole;

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

    /**
     * Pseudo tenant of a PLATFORM_ADMIN assignment: no business data belongs to it.
     */
    public static final String PLATFORM_TENANT = "PLATFORM";

    private final boolean rootReadEnabled;

    public WmpTenantIdentifierResolver() {
        this(false);
    }

    public WmpTenantIdentifierResolver(boolean rootReadEnabled) {
        this.rootReadEnabled = rootReadEnabled;
    }

    /**
     * Whether the caller is a platform admin acting on its pseudo tenant.
     */
    public static boolean isPlatformAdmin(WMPContext context) {
        return context != null
                && PLATFORM_TENANT.equals(context.getTenantId())
                && WmpRole.highestOf(context.getRoles()) == WmpRole.PLATFORM_ADMIN;
    }

    /**
     * Hibernate skips the tenant filter for the root tenant. Opt-in per service, because it opens every bank's rows:
     * {@link TenantScopedEntity} refuses writes in this case, so it only ever widens reads.
     */
    @Override
    public boolean isRoot(String tenantId) {
        return rootReadEnabled
                && PLATFORM_TENANT.equals(tenantId)
                && isPlatformAdmin(SecurityContextHolder.getContext());
    }

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
