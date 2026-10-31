package raff.stein.platformcore.security.jwt;

import org.junit.jupiter.api.Test;
import raff.stein.platformcore.security.jwt.revocation.InMemoryTokenStore;
import raff.stein.platformcore.security.jwt.revocation.TokenStoreProperties;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenRevocationServiceTest {

    private final TokenRevocationService service = new TokenRevocationService(new InMemoryTokenStore(), new TokenStoreProperties());
    private final long exp = Instant.now().getEpochSecond() + 60;

    @Test
    void shouldRejectRevokedToken() {
        service.revoke("jti-1", exp);

        assertTrue(service.isRevoked("jti-1"));
        assertFalse(service.isRevoked("jti-2"));
    }

    @Test
    void shouldConsumeSingleUseTokenOnlyOnce() {
        assertTrue(service.consumeOnce("jti-1", exp));
        assertFalse(service.consumeOnce("jti-1", exp));
    }

    @Test
    void shouldNotTreatConsumedTokenAsRevoked() {
        service.consumeOnce("jti-1", exp);

        assertFalse(service.isRevoked("jti-1"));
    }

    @Test
    void shouldRevokeEveryTokenOfTheUserIssuedUpToTheRevocation() {
        final long revokedAt = Instant.now().getEpochSecond();
        service.revokeAllIssuedUpTo("user-1", revokedAt);

        assertTrue(service.isRevokedForUser("user-1", revokedAt - 60));
        assertTrue(service.isRevokedForUser("user-1", revokedAt));
        assertFalse(service.isRevokedForUser("user-1", revokedAt + 1));
        assertFalse(service.isRevokedForUser("user-2", revokedAt - 60));
    }

    @Test
    void shouldTreatUndatedTokenOfRevokedUserAsRevoked() {
        service.revokeAllIssuedUpTo("user-1", Instant.now().getEpochSecond());

        assertTrue(service.isRevokedForUser("user-1", null));
        assertFalse(service.isRevokedForUser("user-2", null));
    }

    @Test
    void shouldIgnoreBlankJti() {
        service.revoke(" ", exp);

        assertFalse(service.isRevoked(" "));
        assertFalse(service.consumeOnce(null, exp));
    }
}
