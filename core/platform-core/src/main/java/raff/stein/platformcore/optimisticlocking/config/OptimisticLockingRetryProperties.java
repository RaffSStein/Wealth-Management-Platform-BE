package raff.stein.platformcore.optimisticlocking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "optimistic-locking.retry")
public class OptimisticLockingRetryProperties {

    /**
     * Default maximum number of attempts for optimistic locking retries.
     */
    private int maxAttempts = 3;

    /**
     * Initial backoff delay in milliseconds between retry attempts.
     */
    private long backoffDelayMs = 50L;

    /**
     * Multiplier for exponential backoff.
     */
    private double backoffMultiplier = 2.0d;
}

