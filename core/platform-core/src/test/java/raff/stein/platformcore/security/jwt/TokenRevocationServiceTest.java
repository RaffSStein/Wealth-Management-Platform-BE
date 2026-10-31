package raff.stein.platformcore.security.jwt;

import org.junit.jupiter.api.Test;
import raff.stein.platformcore.security.jwt.revocation.InMemoryTokenStore;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenRevocationServiceTest {

    private final TokenRevocationService service = new TokenRevocationService(new InMemoryTokenStore());
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
    void shouldIgnoreBlankJti() {
        service.revoke(" ", exp);

        assertFalse(service.isRevoked(" "));
        assertFalse(service.consumeOnce(null, exp));
    }
}
