package raff.stein.platformcore.messaging.outbox;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import raff.stein.platformcore.messaging.CloudEventExtensions;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

final class OutboxTestEvents {

    static final Instant NOW = Instant.parse("2026-11-02T12:00:00Z");

    private OutboxTestEvents() {
    }

    static CloudEvent cloudEvent(final String id, final String tenantId) {
        return CloudEventBuilder.v1()
                .withId(id)
                .withType("sample.Type")
                .withSource(URI.create("test"))
                .withDataContentType("application/json")
                .withData("{\"value\":\"hello\"}".getBytes(StandardCharsets.UTF_8))
                .withExtension(CloudEventExtensions.TENANT_ID, tenantId)
                .build();
    }

    static OutboxEventEntity pendingEvent(final String id, final String topic) {
        return OutboxEventEntity.builder()
                .eventId(id)
                .topic(topic)
                .payload(OutboxPayloads.serialize(cloudEvent(id, "BANK_A")))
                .nextAttemptAt(NOW)
                .createdAt(NOW)
                .build();
    }
}
