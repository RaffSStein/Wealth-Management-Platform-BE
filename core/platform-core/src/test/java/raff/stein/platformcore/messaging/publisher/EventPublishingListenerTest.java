package raff.stein.platformcore.messaging.publisher;

import io.cloudevents.CloudEvent;
import org.apache.kafka.clients.producer.Producer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import raff.stein.platformcore.messaging.outbox.OutboxEventWriter;
import raff.stein.platformcore.messaging.publisher.model.EventData;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Runs the annotation in a real Spring context with a transaction manager that records what happens, so the order of
 * "store/send" and "commit" is visible for both modes of the outbox.
 */
class EventPublishingListenerTest {

    public record SamplePayload(String value) {
    }

    /** A publisher the way services write them: one method, one annotation, no knowledge of the outbox. */
    static class SamplePublisher {

        private final WMPBaseEventPublisher publisher;

        SamplePublisher(final WMPBaseEventPublisher publisher) {
            this.publisher = publisher;
        }

        @EventPublishingListener
        public void publish(final SamplePayload event) {
            publisher.publishCloudEvent("test-topic", new EventData(event));
        }
    }

    static class RecordingTransactionManager extends AbstractPlatformTransactionManager {

        private final List<String> steps;

        RecordingTransactionManager(final List<String> steps) {
            this.steps = steps;
        }

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(final Object transaction, final TransactionDefinition definition) {
            // nothing to open: the test only needs the synchronization callbacks
        }

        @Override
        protected void doCommit(final DefaultTransactionStatus status) {
            steps.add("commit");
        }

        @Override
        protected void doRollback(final DefaultTransactionStatus status) {
            steps.add("rollback");
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class TransactionConfig {
    }

    private final List<String> steps = new ArrayList<>();
    private AnnotationConfigApplicationContext context;
    private WMPBaseEventPublisher publisher;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        final Producer<String, CloudEvent> producer = mock(Producer.class);
        doAnswer(invocation -> {
            steps.add("kafka-send");
            return null;
        }).when(producer).send(any(), any());
        publisher = new WMPBaseEventPublisher(producer, JsonMapper.builder().build(), "test-service");

        context = new AnnotationConfigApplicationContext();
        context.register(TransactionConfig.class);
        context.registerBean(PlatformTransactionManager.class, () -> new RecordingTransactionManager(steps));
        context.registerBean(SamplePublisher.class, () -> new SamplePublisher(publisher));
        context.refresh();
        transactionTemplate = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    private void turnOutboxOn() {
        final OutboxEventWriter writer = mock(OutboxEventWriter.class);
        doAnswer(invocation -> {
            steps.add("outbox-store");
            return null;
        }).when(writer).store(any(), any());
        publisher.setOutboxEventWriter(writer);
    }

    private void raiseEventInTransaction(final boolean rollback) {
        transactionTemplate.executeWithoutResult(status -> {
            context.publishEvent(new SamplePayload("hello"));
            if (rollback) {
                status.setRollbackOnly();
            }
        });
    }

    @Test
    void shouldStoreInTheOutboxBeforeTheCommitWhenTheOutboxIsOn() {
        turnOutboxOn();

        raiseEventInTransaction(false);

        // the row is written while the transaction is still open, so it commits or rolls back with the data
        assertEquals(List.of("outbox-store", "commit"), steps);
    }

    @Test
    void shouldSendOnlyAfterTheCommitWhenTheOutboxIsOff() {
        raiseEventInTransaction(false);

        assertEquals(List.of("commit", "kafka-send"), steps);
    }

    @Test
    void shouldNotAnnounceARolledBackChangeWithoutTheOutbox() {
        raiseEventInTransaction(true);

        assertEquals(List.of("rollback"), steps);
    }

    @Test
    void shouldNotKeepTheEventOfARolledBackChangeInTheOutbox() {
        turnOutboxOn();

        raiseEventInTransaction(true);

        // BEFORE_COMMIT doesn't run when the transaction is already marked for rollback
        assertEquals(List.of("rollback"), steps);
    }

    @Test
    void shouldStillPublishOutsideATransaction() {
        context.publishEvent(new SamplePayload("hello"));

        assertEquals(List.of("kafka-send"), steps);
    }
}
