package raff.stein.platformcore.messaging.publisher;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the method of a publisher bean that turns an application event, raised inside a business transaction, into a
 * Kafka event through {@link WMPBaseEventPublisher}.
 * <p>
 * The listener runs just before the commit, in the same transaction, with or without the outbox. What happens to the
 * event is decided by {@link WMPBaseEventPublisher#publishCloudEvent}: it is stored in the outbox when
 * {@code platform.outbox.enabled} is true, and sent after the commit when it is not. So turning the outbox on or off for a
 * service never touches its publishers.
 * </p>
 * <p>
 * A phase can't depend on a property: it is fixed by the annotation, and Spring builds the listener from it. This
 * annotation therefore picks the one phase that works in both modes. {@code AFTER_COMMIT} would not, since the outbox row
 * could no longer join the transaction. A call outside a transaction still runs ({@code fallbackExecution}).
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
public @interface EventPublishingListener {
}
