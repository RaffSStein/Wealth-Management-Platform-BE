package raff.stein.platformcore.messaging.consumer;

import io.cloudevents.CloudEvent;

import java.util.Optional;
import java.util.function.Consumer;

public interface EventConsumer {

    <T> Optional<T> getEventPayload(CloudEvent cloudEvent, Class<T> clazz);

    <T> void withEventPayload(CloudEvent cloudEvent, Class<T> clazz, Consumer<T> consumer);
}
