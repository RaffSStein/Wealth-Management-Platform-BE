package raff.stein.platformcore.security.jwt.revocation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(TokenStoreProperties.class)
@Slf4j
public class TokenStoreConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "platform.security.token-store", name = "type", havingValue = "REDIS", matchIfMissing = true)
    public TokenStore redisTokenStore(StringRedisTemplate redisTemplate, TokenStoreProperties properties) {
        log.info("Using Redis token store with key prefix [{}]", properties.getKeyPrefix());
        return new RedisTokenStore(redisTemplate, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.security.token-store", name = "type", havingValue = "IN_MEMORY")
    public TokenStore inMemoryTokenStore() {
        log.warn("Using in-memory token store: revocations are not shared with other services or instances");
        return new InMemoryTokenStore();
    }
}
