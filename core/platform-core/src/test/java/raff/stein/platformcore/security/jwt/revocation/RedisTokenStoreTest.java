package raff.stein.platformcore.security.jwt.revocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import raff.stein.platformcore.exception.types.unavailable.TokenStoreUnavailableException;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisTokenStoreTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisTokenStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        store = new RedisTokenStore(redisTemplate, new TokenStoreProperties());
    }

    @Test
    void shouldWriteKeyWithTtlUntilExpiryPlusClockSkew() {
        final long exp = Instant.now().getEpochSecond() + 600;
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        assertTrue(store.markIfAbsent("revoked", "jti-1", exp));

        final ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).setIfAbsent(eq("wmp:jwt:revoked:jti-1"), eq("1"), ttl.capture());
        final long seconds = ttl.getValue().toSeconds();
        assertTrue(seconds > 600 && seconds <= 660, "ttl was " + seconds);
    }

    @Test
    void shouldReportAlreadyMarked() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertFalse(store.markIfAbsent("setup-used", "jti-1", Instant.now().getEpochSecond() + 60));
    }

    @Test
    void shouldCheckKeyPresence() {
        when(redisTemplate.hasKey("wmp:jwt:revoked:jti-1")).thenReturn(true);

        assertTrue(store.contains("revoked", "jti-1"));
        assertFalse(store.contains("revoked", "jti-2"));
    }

    @Test
    void shouldFailClosedWhenRedisIsDown() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("down"));

        assertThrows(TokenStoreUnavailableException.class, () -> store.contains("revoked", "jti-1"));
        assertThrows(TokenStoreUnavailableException.class,
                () -> store.markIfAbsent("setup-used", "jti-1", Instant.now().getEpochSecond() + 60));
    }

    @Test
    void shouldUseConfiguredKeyPrefix() {
        final TokenStoreProperties properties = new TokenStoreProperties();
        properties.setKeyPrefix("test");

        assertEquals("test:revoked:jti-1", new RedisTokenStore(redisTemplate, properties).key("revoked", "jti-1"));
    }
}
