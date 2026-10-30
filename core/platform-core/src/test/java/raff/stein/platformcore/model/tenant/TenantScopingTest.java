package raff.stein.platformcore.model.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import raff.stein.platformcore.exception.types.unauthorized.WmpContextException;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
