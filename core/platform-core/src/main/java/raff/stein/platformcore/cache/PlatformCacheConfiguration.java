package raff.stein.platformcore.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import raff.stein.platformcore.cache.PlatformCacheProperties.Provider;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

/**
 * Shared cache configuration that exposes a {@link CacheManager} for all services.
 *
 * <p>It is opt-in and controlled via {@code platform.cache.*} properties.
 */
@Configuration
@EnableConfigurationProperties(PlatformCacheProperties.class)
@ConditionalOnProperty(prefix = "platform.cache", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class PlatformCacheConfiguration {

    private final PlatformCacheProperties cacheProperties;
    private final Environment environment;

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        Provider provider = cacheProperties.getProvider();
        if (provider == null) {
            provider = Provider.NONE;
        }

        log.info("Configuring platform CacheManager with provider={} and enabled=true", provider);

        return switch (provider) {
            case REDIS -> new NoOpCacheManager(); // real Redis manager is configured in Redis-specific configuration
            case SIMPLE_IN_MEMORY -> createInMemoryCacheManager();
            case NONE -> new NoOpCacheManager();
        };
    }

    private CacheManager createInMemoryCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(predefinedCacheNames());
        log.info("Using SIMPLE_IN_MEMORY ConcurrentMapCacheManager for caches={}", cacheManager.getCacheNames());
        return cacheManager;
    }

    private Collection<String> predefinedCacheNames() {
        Map<String, PlatformCacheProperties.CacheConfig> caches = cacheProperties.getCaches();
        if (caches == null || caches.isEmpty()) {
            return Collections.emptyList();
        }
        return new HashSet<>(caches.keySet());
    }

    /**
     * Resolve the logical service name used for cache namespacing.
     */
    public String resolveServiceName() {
        if (cacheProperties.getServiceName() != null && !cacheProperties.getServiceName().isBlank()) {
            return cacheProperties.getServiceName();
        }
        String appName = environment.getProperty("spring.application.name");
        return appName != null ? appName : "unknown-service";
    }

    /**
     * Build a namespaced cache name including optional global prefix and service name.
     */
    public String buildCacheName(String logicalName) {
        String serviceName = resolveServiceName();
        String keyPrefix = cacheProperties.getKeyPrefix();

        StringBuilder builder = new StringBuilder();
        if (keyPrefix != null && !keyPrefix.isBlank()) {
            builder.append(keyPrefix).append(":");
        }
        builder.append(serviceName).append("::").append(logicalName);
        return builder.toString();
    }
}
