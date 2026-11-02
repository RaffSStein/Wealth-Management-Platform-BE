package raff.stein.platformcore.messaging.outbox;

import io.cloudevents.CloudEvent;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OutboxRelayTest {

    private OutboxEventRepository repository;
    private Producer<String, CloudEvent> producer;
    private OutboxProperties properties;
    private OutboxRelay relay;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        producer = mock(Producer.class);
        properties = new OutboxProperties();
        properties.setInitialBackoffMs(2_000L);
        properties.setMaxBackoffMs(10_000L);
        properties.setMaxAttempts(5);
        relay = new OutboxRelay(repository, producer, properties, mock(PlatformTransactionManager.class),
                Clock.fixed(OutboxTestEvents.NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void stopRelay() {
        relay.shutdown();
    }

    private void givenDue(final OutboxEventEntity... events) {
        when(repository.findDue(eq(OutboxStatus.PENDING), eq(OutboxTestEvents.NOW), any(Pageable.class)))
                .thenReturn(List.of(events));
    }

    private void givenBrokerAcknowledges() {
        when(producer.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(mock(RecordMetadata.class)));
    }

    private void givenBrokerFails(final String reason) {
        when(producer.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.failedFuture(new RuntimeException(reason)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldDeleteTheEventOnlyAfterTheBrokerAcknowledged() {
        final OutboxEventEntity event = OutboxTestEvents.pendingEvent("event-1", "test-topic");
        givenDue(event);
        givenBrokerAcknowledges();

        relay.drain();

        final ArgumentCaptor<ProducerRecord<String, CloudEvent>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(sent.capture());
        assertEquals("test-topic", sent.getValue().topic());
        // same CloudEvent id on every delivery, so consumers can spot a replay
        assertEquals("event-1", sent.getValue().value().getId());
        verify(repository).deleteAll(List.of(event));
    }

    @Test
    void shouldKeepTheEventAndBackOffWhenTheSendFails() {
        final OutboxEventEntity event = OutboxTestEvents.pendingEvent("event-1", "test-topic");
        givenDue(event);
        givenBrokerFails("broker down");

        relay.drain();

        assertEquals(1, event.getAttempts());
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(OutboxTestEvents.NOW.plusMillis(2_000L), event.getNextAttemptAt());
        assertTrue(event.getLastError().contains("broker down"));
        verify(repository).deleteAll(List.of());
    }

    @Test
    void shouldDoubleTheDelayAfterEachFailure() {
        final OutboxEventEntity event = OutboxTestEvents.pendingEvent("event-1", "test-topic");
        event.setAttempts(2);
        givenDue(event);
        givenBrokerFails("broker down");

        relay.drain();

        // third failure: 2s * 2^2
        assertEquals(3, event.getAttempts());
        assertEquals(OutboxTestEvents.NOW.plusMillis(8_000L), event.getNextAttemptAt());
    }

    @Test
    void shouldCapTheDelay() {
        final OutboxEventEntity event = OutboxTestEvents.pendingEvent("event-1", "test-topic");
        event.setAttempts(3);
        givenDue(event);
        givenBrokerFails("broker down");

        relay.drain();

        // 2s * 2^3 = 16s, capped at 10s
        assertEquals(OutboxTestEvents.NOW.plusMillis(10_000L), event.getNextAttemptAt());
    }

    @Test
    void shouldGiveUpAfterTheMaximumNumberOfAttempts() {
        final OutboxEventEntity event = OutboxTestEvents.pendingEvent("event-1", "test-topic");
        event.setAttempts(4);
        givenDue(event);
        givenBrokerFails("broker down");

        relay.drain();

        assertEquals(5, event.getAttempts());
        assertEquals(OutboxStatus.FAILED, event.getStatus());
        // not rescheduled: FAILED rows are never selected again
        assertEquals(OutboxTestEvents.NOW, event.getNextAttemptAt());
    }

    @Test
    void shouldCountAnUnreadablePayloadAsAFailureWithoutSending() {
        final OutboxEventEntity corrupt = OutboxEventEntity.builder()
                .eventId("event-1")
                .topic("test-topic")
                .payload("not a cloud event")
                .nextAttemptAt(OutboxTestEvents.NOW)
                .createdAt(OutboxTestEvents.NOW)
                .build();
        final OutboxEventEntity good = OutboxTestEvents.pendingEvent("event-2", "test-topic");
        givenDue(corrupt, good);
        givenBrokerAcknowledges();

        relay.drain();

        assertEquals(1, corrupt.getAttempts());
        // one bad row doesn't hold up the others
        verify(producer).send(any(ProducerRecord.class));
        verify(repository).deleteAll(List.of(good));
    }

    @Test
    void shouldStopTheBatchAtTheFirstSendThatFailsAtOnce() {
        final OutboxEventEntity first = OutboxTestEvents.pendingEvent("event-1", "test-topic");
        final OutboxEventEntity second = OutboxTestEvents.pendingEvent("event-2", "test-topic");
        givenDue(first, second);
        givenBrokerFails("broker down");

        relay.drain();

        // send() already waited for the unreachable broker: trying the rest would hold the transaction just as long
        verify(producer).send(any(ProducerRecord.class));
        assertEquals(1, first.getAttempts());
        // not tried, so no attempt is counted against it
        assertEquals(0, second.getAttempts());
        assertEquals(OutboxTestEvents.NOW, second.getNextAttemptAt());
    }

    @Test
    void shouldCountTheRetryDelayFromTheFailureNotFromTheStartOfTheRound() {
        final AtomicReference<Instant> time = new AtomicReference<>(OutboxTestEvents.NOW);
        final Clock movingClock = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(final java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return time.get();
            }
        };
        final OutboxRelay slowRelay = new OutboxRelay(repository, producer, properties,
                mock(PlatformTransactionManager.class), movingClock);
        final OutboxEventEntity event = OutboxTestEvents.pendingEvent("event-1", "test-topic");
        givenDue(event);
        // a send that blocks for three minutes before it fails, like an unreachable broker
        when(producer.send(any(ProducerRecord.class))).thenAnswer(invocation -> {
            time.set(OutboxTestEvents.NOW.plusSeconds(180));
            return CompletableFuture.failedFuture(new RuntimeException("broker down"));
        });

        try {
            slowRelay.drain();
        } finally {
            slowRelay.shutdown();
        }

        assertEquals(OutboxTestEvents.NOW.plusSeconds(180).plusMillis(2_000L), event.getNextAttemptAt());
    }

    @Test
    void shouldDoNothingWhenNothingIsDue() {
        givenDue();

        relay.drain();

        verifyNoInteractions(producer);
        verify(repository, never()).deleteAll(any());
    }
}
