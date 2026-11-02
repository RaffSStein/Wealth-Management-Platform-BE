package raff.stein.platformcore.messaging.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "platform.outbox")
public class OutboxProperties {

    /**
     * Off by default: events go straight to Kafka. Turn it on per service, which then needs a datasource.
     */
    private boolean enabled = false;

    /**
     * Delay between two polls of the outbox table.
     */
    private long pollIntervalMs = 1000L;

    /**
     * Events locked and sent in one transaction.
     */
    private int batchSize = 50;

    /**
     * Attempts before an event is marked FAILED and left alone.
     */
    private int maxAttempts = 10;

    /**
     * How long to wait for the broker's acks of one batch.
     */
    private long sendTimeoutMs = 10_000L;

    /**
     * Delay before the second attempt. It doubles after every failure.
     */
    private long initialBackoffMs = 2_000L;

    /**
     * Upper bound of the retry delay.
     */
    private long maxBackoffMs = 300_000L;
}
