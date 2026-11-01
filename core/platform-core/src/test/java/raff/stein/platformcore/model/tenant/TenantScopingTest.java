package raff.stein.platformcore.model.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;
import raff.stein.platformcore.exception.types.unauthorized.WmpContextException;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantScopingTest {

    private final WmpTenantIdentifierResolver resolver = new WmpTenantIdentifierResolver();

    private static class SampleTenantEntity extends TenantScopedEntity<Long> {
        @Override
        public Long getId() {
            return 1L;
        }
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    void shouldResolveTenantFromContext() {
        SecurityContextHolder.setContext(WMPContext.builder().tenantId("BANK_A").build());

        assertEquals("BANK_A", resolver.resolveCurrentTenantIdentifier());
    }

    @Test
    void shouldResolvePlaceholderWhenNoContext() {
        assertEquals(WmpTenantIdentifierResolver.NO_TENANT, resolver.resolveCurrentTenantIdentifier());
    }

    @Test
    void shouldResolvePlaceholderWhenContextHasNoTenant() {
        SecurityContextHolder.setContext(WMPContext.builder().email("customer@bank.test").build());

        assertEquals(WmpTenantIdentifierResolver.NO_TENANT, resolver.resolveCurrentTenantIdentifier());
    }

    @Test
    void shouldRejectPersistWithoutTenantInContext() {
        final SampleTenantEntity entity = new SampleTenantEntity();

        assertThrows(WmpContextException.class, entity::requireTenantInContext);
    }

    @Test
    void shouldAllowPersistWithTenantInContext() {
        SecurityContextHolder.setContext(WMPContext.builder().tenantId("BANK_A").build());
        final SampleTenantEntity entity = new SampleTenantEntity();

        assertDoesNotThrow(entity::requireTenantInContext);
    }

    private static void signIn(String role, String tenant) {
        SecurityContextHolder.setContext(WMPContext.builder().roles(Set.of(role)).tenantId(tenant).build());
    }

    @Test
    void shouldBeRootOnlyForPlatformAdminOnPlatformTenantWhenEnabled() {
        final WmpTenantIdentifierResolver enabled = new WmpTenantIdentifierResolver(true);

        signIn("PLATFORM_ADMIN", "PLATFORM");
        assertTrue(enabled.isRoot("PLATFORM"));
        assertFalse(enabled.isRoot("BANK_A"));
        assertFalse(resolver.isRoot("PLATFORM"), "off unless the service opts in");

        signIn("BANK_ADMIN", "PLATFORM");
        assertFalse(enabled.isRoot("PLATFORM"));

        signIn("PLATFORM_ADMIN", "BANK_A");
        assertFalse(enabled.isRoot("BANK_A"));

        SecurityContextHolder.clear();
        assertFalse(enabled.isRoot("PLATFORM"));
    }

    @Test
    void shouldRefuseWritesAsPlatformAdmin() {
        signIn("PLATFORM_ADMIN", "PLATFORM");
        final SampleTenantEntity entity = new SampleTenantEntity();

        assertThrows(AccessDeniedException.class, entity::requireTenantInContext);
        assertThrows(AccessDeniedException.class, entity::refuseWriteAsPlatformAdmin);
    }

    @Test
    void shouldAllowWritesAsBankStaff() {
        signIn("BANK_ADMIN", "BANK_A");

        assertDoesNotThrow(new SampleTenantEntity()::refuseWriteAsPlatformAdmin);
    }
}
