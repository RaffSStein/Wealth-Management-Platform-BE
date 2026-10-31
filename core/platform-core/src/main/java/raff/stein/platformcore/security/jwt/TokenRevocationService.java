package raff.stein.platformcore.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import raff.stein.platformcore.security.jwt.revocation.TokenStore;
import raff.stein.platformcore.security.jwt.revocation.TokenStoreProperties;

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
    static final String USER_REVOKED_BEFORE = "user-revoked-before";

    private final TokenStore tokenStore;
    private final TokenStoreProperties properties;

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

    /**
     * Revokes every token of the user issued at or before {@code epochSeconds} (e.g. a removed membership).
     * Kept for the longest token lifetime: by then every older token has expired anyway.
     */
    public void revokeAllIssuedUpTo(String userId, long epochSeconds) {
        if (userId == null || userId.isBlank()) return;
        tokenStore.put(USER_REVOKED_BEFORE, userId, epochSeconds,
                epochSeconds + properties.getMaxTokenLifetime().toSeconds());
    }

    /**
     * True when the user's tokens issued at or before a user-wide revocation are presented.
     * A token without iat can't be dated, so it counts as revoked once the user has a revocation.
     */
    public boolean isRevokedForUser(String userId, Long issuedAtEpochSeconds) {
        if (userId == null || userId.isBlank()) return false;
        final var revokedUpTo = tokenStore.get(USER_REVOKED_BEFORE, userId);
        return revokedUpTo.isPresent()
                && (issuedAtEpochSeconds == null || issuedAtEpochSeconds <= revokedUpTo.getAsLong());
    }
}
