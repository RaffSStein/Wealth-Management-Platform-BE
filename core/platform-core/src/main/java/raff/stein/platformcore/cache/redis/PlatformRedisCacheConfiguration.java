package raff.stein.platformcore.cache.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import raff.stein.platformcore.cache.PlatformCacheConfiguration;
import raff.stein.platformcore.cache.PlatformCacheProperties;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis-backed {@link CacheManager} configuration shared across services.
 */
@Configuration
@EnableConfigurationProperties(PlatformCacheProperties.class)
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "platform.cache", name = {"enabled", "provider"}, havingValue = "true,REDIS")
@RequiredArgsConstructor
@Slf4j
public class PlatformRedisCacheConfiguration {

    private final PlatformCacheProperties cacheProperties;
    private final PlatformCacheConfiguration platformCacheConfiguration;

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = defaultRedisCacheConfiguration();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        for (Map.Entry<String, PlatformCacheProperties.CacheConfig> entry : cacheProperties.getCaches().entrySet()) {
            String logicalName = entry.getKey();
            PlatformCacheProperties.CacheConfig cacheConfig = entry.getValue();

            Duration ttl = cacheConfig.getTtl() != null
                    ? cacheConfig.getTtl()
                    : cacheProperties.getDefaultTtl();

            RedisCacheConfiguration specificConfig = defaultConfig;
            if (ttl != null) {
                specificConfig = defaultConfig.entryTtl(ttl);
            }

            String cacheName = platformCacheConfiguration.buildCacheName(logicalName);
            cacheConfigurations.put(cacheName, specificConfig);
        }

        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager
                .builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations);

        log.info("Using RedisCacheManager with {} predefined caches", cacheConfigurations.size());
        return builder.build();
    }

    private RedisCacheConfiguration defaultRedisCacheConfiguration() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        ObjectMapper objectMapper = new ObjectMapper();
        config = config.serializeValuesWith(
                SerializationPair.fromSerializer(new GenericJacksonJsonRedisSerializer(objectMapper)));

        Duration defaultTtl = cacheProperties.getDefaultTtl();
        if (defaultTtl != null) {
            config = config.entryTtl(defaultTtl);
        }

        return config;
    }
}
