package raff.stein.platformcore.messaging.outbox;

import io.cloudevents.CloudEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Delivers the outbox to Kafka, at least once: an event is deleted only after the broker acknowledged it, so a crash
 * in between sends it again with the same CloudEvent id.
 * <p>
 * Each round locks a batch with "skip locked", sends it, and waits for the acks. A failed event waits with an
 * exponential backoff, and after {@code maxAttempts} it is marked FAILED and left for a person to look at.
 * </p>
 */
@Slf4j
public class OutboxRelay {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final OutboxEventRepository repository;
    private final Producer<String, CloudEvent> producer;
    private final OutboxProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final AtomicBoolean draining = new AtomicBoolean(false);
    private final ExecutorService wakeUpExecutor = Executors.newSingleThreadExecutor(runnable -> {
        final Thread thread = new Thread(runnable, "outbox-relay");
        thread.setDaemon(true);
        return thread;
    });

    public OutboxRelay(final OutboxEventRepository repository,
                       final Producer<String, CloudEvent> producer,
                       final OutboxProperties properties,
                       final PlatformTransactionManager transactionManager,
                       final Clock clock) {
        this.repository = repository;
        this.producer = producer;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /** Asks for a delivery round now, on the relay's own thread. */
    public void wakeUp() {
        wakeUpExecutor.execute(this::drain);
    }

    public void shutdown() {
        wakeUpExecutor.shutdownNow();
    }

    @Scheduled(fixedDelayString = "${platform.outbox.poll-interval-ms:1000}")
    public void drain() {
        // one round at a time per instance; other instances are kept apart by the row locks
        if (!draining.compareAndSet(false, true)) {
            return;
        }
        try {
            int processed;
            do {
                final Integer count = transactionTemplate.execute(status -> deliverBatch());
                processed = count == null ? 0 : count;
            } while (processed >= properties.getBatchSize() && !Thread.currentThread().isInterrupted());
        } catch (RuntimeException e) {
            // e.g. the database is down: the next poll tries again
            log.error("Outbox delivery round failed: {}", e.getMessage(), e);
        } finally {
            draining.set(false);
        }
    }

    private int deliverBatch() {
        final Instant now = clock.instant();
        final List<OutboxEventEntity> due = repository.findDue(
                OutboxStatus.PENDING, now, PageRequest.of(0, properties.getBatchSize()));
        if (due.isEmpty()) {
            return 0;
        }
        // send the whole batch first and wait afterwards, so the transaction is open for one timeout, not one per event
        final List<OutboxEventEntity> delivered = new ArrayList<>();
        final List<InFlight> inFlight = new ArrayList<>();
        boolean brokerUnavailable = false;
        for (final OutboxEventEntity event : due) {
            try {
                final CloudEvent cloudEvent = OutboxPayloads.deserialize(event.getPayload());
                final Future<RecordMetadata> ack = producer.send(new ProducerRecord<>(event.getTopic(), cloudEvent));
                inFlight.add(new InFlight(event, ack));
                // send() blocks while the broker can't be reached, then hands back a failed future. The rest of the
                // batch would wait just as long, with the transaction and the row locks held, so stop here: the
                // events not tried stay untouched and are picked up by a later round.
                if (failedAlready(ack)) {
                    brokerUnavailable = true;
                    break;
                }
            } catch (RuntimeException e) {
                recordFailure(event, e);
            }
        }
        final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(properties.getSendTimeoutMs());
        for (final InFlight sent : inFlight) {
            try {
                final long remaining = Math.max(0, deadline - System.nanoTime());
                sent.ack().get(remaining, TimeUnit.NANOSECONDS);
                delivered.add(sent.event());
            } catch (InterruptedException e) {
                // shutting down: what is not acked stays in the outbox
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException | TimeoutException | RuntimeException e) {
                recordFailure(sent.event(), e);
            }
        }
        repository.deleteAll(delivered);
        log.info("Outbox round: delivered [{}] of [{}] events", delivered.size(), due.size());
        if (brokerUnavailable) {
            log.warn("Kafka is not reachable: the rest of the batch is left for the next round");
            // 0 ends this drain: looping again would only wait for the same broker
            return 0;
        }
        return due.size();
    }

    private static boolean failedAlready(final Future<RecordMetadata> ack) {
        if (!ack.isDone()) {
            return false;
        }
        try {
            ack.get();
            return false;
        } catch (ExecutionException e) {
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }
    }

    private void recordFailure(final OutboxEventEntity event, final Exception error) {
        // the clock is read now: a round can last a long time, and the delay counts from the failure
        final Instant now = clock.instant();
        final int attempts = event.getAttempts() + 1;
        event.setAttempts(attempts);
        event.setLastError(truncate(String.valueOf(error.getMessage() == null ? error : error.getMessage())));
        if (attempts >= properties.getMaxAttempts()) {
            event.setStatus(OutboxStatus.FAILED);
            log.error("Outbox event given up after [{}] attempts, eventId: [{}], topic: [{}], error: {}",
                    attempts, event.getEventId(), event.getTopic(), event.getLastError());
            return;
        }
        event.setNextAttemptAt(now.plusMillis(backoffMs(attempts)));
        log.warn("Outbox event not delivered, eventId: [{}], topic: [{}], attempt: [{}], error: {}",
                event.getEventId(), event.getTopic(), attempts, event.getLastError());
    }

    // doubles after every failure, up to the cap; the shift is bounded so it can't overflow
    private long backoffMs(final int attempts) {
        final long delay = properties.getInitialBackoffMs() << Math.min(attempts - 1, 20);
        return Math.min(delay, properties.getMaxBackoffMs());
    }

    private static String truncate(final String text) {
        return text.length() <= MAX_ERROR_LENGTH ? text : text.substring(0, MAX_ERROR_LENGTH);
    }

    private record InFlight(OutboxEventEntity event, Future<RecordMetadata> ack) {
    }
}
