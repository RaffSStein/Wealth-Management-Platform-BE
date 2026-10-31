package raff.stein.platformcore.security.jwt.revocation;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryTokenStoreTest {

    private final InMemoryTokenStore store = new InMemoryTokenStore();

    @Test
    void shouldMarkOnlyOnce() {
        final long exp = Instant.now().getEpochSecond() + 60;

        assertTrue(store.markIfAbsent("revoked", "jti-1", exp));
        assertFalse(store.markIfAbsent("revoked", "jti-1", exp));
        assertTrue(store.contains("revoked", "jti-1"));
    }

    @Test
    void shouldKeepNamespacesApart() {
        store.markIfAbsent("revoked", "jti-1", Instant.now().getEpochSecond() + 60);

        assertFalse(store.contains("setup-used", "jti-1"));
    }

    @Test
    void shouldForgetExpiredEntries() {
        final long alreadyExpired = Instant.now().getEpochSecond() - 1;
        store.markIfAbsent("revoked", "jti-1", alreadyExpired);

        assertFalse(store.contains("revoked", "jti-1"));
        assertTrue(store.markIfAbsent("revoked", "jti-1", Instant.now().getEpochSecond() + 60));
    }
}
