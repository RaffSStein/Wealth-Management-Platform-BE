package raff.stein.platformcore.messaging.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * An event waiting to be sent to Kafka, stored in the same transaction as the business write.
 * <p>
 * Deliberately not a {@code TenantScopedEntity}: the relay runs with no tenant, so the tenant filter would hide every
 * row. The tenant travels inside the serialized CloudEvent. It is not audited either, since consumers have no user.
 * </p>
 */
@Entity
@Table(name = "event_outbox",
        uniqueConstraints = @UniqueConstraint(name = "uk_event_outbox_event_id", columnNames = "event_id"),
        indexes = @Index(name = "idx_event_outbox_due", columnList = "status, next_attempt_at, id"))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The CloudEvent id: stays the same on every redelivery, so consumers can recognize a replay. */
    @Column(name = "event_id", nullable = false, updatable = false, length = 64)
    private String eventId;

    @Column(nullable = false, updatable = false, length = 512)
    private String topic;

    /** The whole CloudEvent in its JSON format, extensions included. */
    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String payload;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Setter
    @Column(nullable = false)
    private int attempts;

    @Setter
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Setter
    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
