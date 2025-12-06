package raff.stein.platformcore.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared cache configuration properties for all platform services.
 */
@Data
@ConfigurationProperties(prefix = "platform.cache")
public class PlatformCacheProperties {

    /**
     * Master switch to enable or disable caching for the service.
     */
    private boolean enabled = false;

    /**
     * Cache provider to use. Defaults to NONE for backward compatibility.
     */
    private Provider provider = Provider.NONE;

    /**
     * Default time-to-live for cache entries. If null, entries do not expire by default.
     */
    private Duration defaultTtl;

    /**
     * Optional global key prefix applied to all cache names.
     */
    private String keyPrefix;

    /**
     * Optional service name override. If not set, spring.application.name will be used when available.
     */
    private String serviceName;

    /**
     * Per-cache specific configuration.
     */
    @NestedConfigurationProperty
    private Map<String, CacheConfig> caches = new HashMap<>();

    @Data
    public static class CacheConfig {

        /**
         * Time-to-live for this specific cache. Overrides {@link #defaultTtl} when set.
         */
        private Duration ttl;
    }

    public enum Provider {
        NONE,
        REDIS,
        SIMPLE_IN_MEMORY
    }
}

