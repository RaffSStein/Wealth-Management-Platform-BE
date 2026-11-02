package raff.stein.platformcore.messaging.outbox;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;

import java.nio.charset.StandardCharsets;

/**
 * Converts a CloudEvent to the text stored in the outbox and back, using the standard JSON event format.
 */
final class OutboxPayloads {

    private static final EventFormat FORMAT = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);

    private OutboxPayloads() {
    }

    static String serialize(final CloudEvent cloudEvent) {
        return new String(FORMAT.serialize(cloudEvent), StandardCharsets.UTF_8);
    }

    static CloudEvent deserialize(final String payload) {
        return FORMAT.deserialize(payload.getBytes(StandardCharsets.UTF_8));
    }
}
