package raff.stein.platformcore.security.jwt.revocation;

import java.time.Instant;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-instance store: entries are not shared with other services or instances and are lost on restart.
 */
public class InMemoryTokenStore implements TokenStore {

    private final Map<String, Long> entries = new ConcurrentHashMap<>(); // namespace:jti -> expEpochSeconds
    private final Map<String, long[]> values = new ConcurrentHashMap<>(); // namespace:key -> {value, expEpochSeconds}

    @Override
    public boolean markIfAbsent(String namespace, String jti, long expEpochSeconds) {
        final String key = key(namespace, jti);
        // an expired entry doesn't count as present
        evictIfExpired(key);
        return entries.putIfAbsent(key, expEpochSeconds) == null;
    }

    @Override
    public boolean contains(String namespace, String jti) {
        final String key = key(namespace, jti);
        evictIfExpired(key);
        return entries.containsKey(key);
    }

    @Override
    public void put(String namespace, String key, long value, long expEpochSeconds) {
        values.put(key(namespace, key), new long[]{value, expEpochSeconds});
    }

    @Override
    public OptionalLong get(String namespace, String key) {
        final long now = Instant.now().getEpochSecond();
        final long[] entry = values.computeIfPresent(key(namespace, key), (k, v) -> now >= v[1] ? null : v);
        return entry == null ? OptionalLong.empty() : OptionalLong.of(entry[0]);
    }

    private void evictIfExpired(String key) {
        final long now = Instant.now().getEpochSecond();
        entries.computeIfPresent(key, (k, exp) -> now >= exp ? null : exp);
    }

    private static String key(String namespace, String jti) {
        return namespace + ":" + jti;
    }
}
