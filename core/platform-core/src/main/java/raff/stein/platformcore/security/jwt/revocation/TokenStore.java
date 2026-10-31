package raff.stein.platformcore.security.jwt.revocation;

import java.util.OptionalLong;

/**
 * Stores token ids (jti) until the token expires. Entries are grouped by namespace, e.g. revoked session tokens or
 * consumed single-use tokens.
 */
public interface TokenStore {

    /**
     * Records the jti until {@code expEpochSeconds}. Atomic: returns true only for the first write.
     */
    boolean markIfAbsent(String namespace, String jti, long expEpochSeconds);

    /**
     * True when the jti is recorded and not yet expired.
     */
    boolean contains(String namespace, String jti);

    /**
     * Stores a numeric value until {@code expEpochSeconds}, replacing any previous one.
     */
    void put(String namespace, String key, long value, long expEpochSeconds);

    /**
     * The value stored for the key, if present and not yet expired.
     */
    OptionalLong get(String namespace, String key);
}
