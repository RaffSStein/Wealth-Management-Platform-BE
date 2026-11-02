package raff.stein.platformcore.messaging.outbox;

public enum OutboxStatus {
    /** Waiting for delivery, or for the next retry. */
    PENDING,
    /** Gave up after the maximum number of attempts. Kept for inspection, never retried automatically. */
    FAILED
}
