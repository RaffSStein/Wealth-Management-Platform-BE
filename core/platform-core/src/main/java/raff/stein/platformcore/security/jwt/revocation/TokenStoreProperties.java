package raff.stein.platformcore.security.jwt.revocation;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "platform.security.token-store")
public class TokenStoreProperties {

    /**
     * REDIS shares the store across services and instances. IN_MEMORY is per instance (tests, isolated runs).
     */
    private Type type = Type.REDIS;

    /**
     * Prefix of every Redis key: {@code <keyPrefix>:<namespace>:<jti>}.
     */
    private String keyPrefix = "wmp:jwt";

    /**
     * Kept on top of the token's remaining life, so a service whose clock runs behind still sees the entry.
     */
    private Duration clockSkew = Duration.ofSeconds(60);

    public enum Type {
        REDIS,
        IN_MEMORY
    }
}
