package raff.stein.platformcore.messaging.outbox;

import io.cloudevents.CloudEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;

/**
 * Stores an event in the outbox. It joins the caller's transaction, so the event exists if and only if the business
 * write commits. A caller without a transaction gets one of its own: the event is still delivered at least once, but
 * it is not atomic with the caller's writes.
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxEventWriter {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxRelay outboxRelay;
    private final Clock clock;

    @Transactional
    public void store(@NonNull final String topic, @NonNull final CloudEvent cloudEvent) {
        final Instant now = clock.instant();
        outboxEventRepository.save(OutboxEventEntity.builder()
                .eventId(cloudEvent.getId())
                .topic(topic)
                .payload(OutboxPayloads.serialize(cloudEvent))
                .nextAttemptAt(now)
                .createdAt(now)
                .build());
        log.info("CloudEvent stored in the outbox for topic: [{}], cloudEventId: [{}], cloudEventType: [{}]",
                topic, cloudEvent.getId(), cloudEvent.getType());
        wakeRelayAfterCommit();
    }

    // delivery shouldn't wait for the next poll, but it must not start before the row is visible
    private void wakeRelayAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    outboxRelay.wakeUp();
                }
            });
        }
    }
}
