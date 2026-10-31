package raff.stein.platformcore.security.jwt.revocation;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import raff.stein.platformcore.exception.types.unavailable.TokenStoreUnavailableException;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;

/**
 * Store shared by every service and instance. Each entry lives until the token expires (plus the clock skew),
 * so Redis cleans it up and no scheduler is needed.
 */
@RequiredArgsConstructor
public class RedisTokenStore implements TokenStore {

    private static final String PRESENT = "1";

    private final StringRedisTemplate redisTemplate;
    private final TokenStoreProperties properties;

    @Override
    public boolean markIfAbsent(String namespace, String jti, long expEpochSeconds) {
        try {
            final Boolean written = redisTemplate.opsForValue()
                    .setIfAbsent(key(namespace, jti), PRESENT, ttl(expEpochSeconds));
            return Boolean.TRUE.equals(written);
        } catch (DataAccessException e) {
            throw new TokenStoreUnavailableException("mark " + namespace, e);
        }
    }

    @Override
    public boolean contains(String namespace, String jti) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key(namespace, jti)));
        } catch (DataAccessException e) {
            throw new TokenStoreUnavailableException("check " + namespace, e);
        }
    }

    @Override
    public void put(String namespace, String key, long value, long expEpochSeconds) {
        try {
            redisTemplate.opsForValue().set(key(namespace, key), Long.toString(value), ttl(expEpochSeconds));
        } catch (DataAccessException e) {
            throw new TokenStoreUnavailableException("put " + namespace, e);
        }
    }

    @Override
    public OptionalLong get(String namespace, String key) {
        try {
            final String value = redisTemplate.opsForValue().get(key(namespace, key));
            return value == null ? OptionalLong.empty() : OptionalLong.of(Long.parseLong(value));
        } catch (DataAccessException e) {
            throw new TokenStoreUnavailableException("get " + namespace, e);
        }
    }

    String key(String namespace, String jti) {
        return properties.getKeyPrefix() + ":" + namespace + ":" + jti;
    }

    private Duration ttl(long expEpochSeconds) {
        final long remaining = Math.max(0, expEpochSeconds - Instant.now().getEpochSecond());
        return Duration.ofSeconds(remaining).plus(properties.getClockSkew());
    }
}
