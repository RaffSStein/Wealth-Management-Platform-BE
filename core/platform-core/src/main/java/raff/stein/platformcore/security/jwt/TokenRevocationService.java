package raff.stein.platformcore.security.jwt;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token revocation store. Keeps track of revoked JWT IDs (jti) until they expire.
 */

@Service
public class TokenRevocationService {

    private final Map<String, Long> revoked = new ConcurrentHashMap<>(); // jti -> expEpochSeconds

    /**
     * Revoke a token given its jti and expiration epoch seconds.
     */
    public void revoke(String jti, long expEpochSeconds) {
        if (jti == null || jti.isBlank()) return;
        // TODO: in production, consider a persistent store or distributed cache
        revoked.putIfAbsent(jti, expEpochSeconds);
    }

    /**
     * Returns true if the jti has been revoked and not yet expired.
     */
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) return false;
        Long exp = revoked.get(jti);
        if (exp == null) return false;
        long now = Instant.now().getEpochSecond();
        if (now >= exp) {
            // cleanup expired entry lazily
            revoked.remove(jti);
            return false;
        }
        return true;
    }
}

