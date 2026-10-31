package raff.stein.platformcore.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import raff.stein.platformcore.security.jwt.revocation.TokenStore;

/**
 * Tracks token ids (jti) that must no longer be accepted: revoked session tokens and consumed single-use tokens.
 * Backed by the shared {@link TokenStore}, so a token revoked by one service is rejected by all of them.
 * Every method throws {@code TokenStoreUnavailableException} when the store can't be reached: callers fail closed.
 */
@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    static final String REVOKED = "revoked";
    static final String SINGLE_USE_CONSUMED = "setup-used";

    private final TokenStore tokenStore;

    /**
     * Revoke a token given its jti and expiration epoch seconds.
     */
    public void revoke(String jti, long expEpochSeconds) {
        if (jti == null || jti.isBlank()) return;
        tokenStore.markIfAbsent(REVOKED, jti, expEpochSeconds);
    }

    /**
     * Returns true if the jti has been revoked and not yet expired.
     */
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) return false;
        return tokenStore.contains(REVOKED, jti);
    }

    /**
     * Consumes a single-use token. Returns true on the first use only, across all instances.
     */
    public boolean consumeOnce(String jti, long expEpochSeconds) {
        if (jti == null || jti.isBlank()) return false;
        return tokenStore.markIfAbsent(SINGLE_USE_CONSUMED, jti, expEpochSeconds);
    }
}
