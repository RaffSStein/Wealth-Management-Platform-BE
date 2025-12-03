package raff.stein.platformcore.messaging.consumer;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class WMPBaseEventConsumer implements EventConsumer {

    private ObjectMapper objectMapper;

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> Optional<T> getEventPayload(CloudEvent cloudEvent, Class<T> clazz) {
        log.info("CloudEvent received for class: [{}], eventId; [{}]",
                clazz.getName(),
                cloudEvent.getId());
        log.debug("CloudEvent received for class: [{}], eventId; [{}], data: [{}]",
                clazz.getName(),
                cloudEvent.getId(),
                cloudEvent.getData());
        CloudEventData cloudEventData = cloudEvent.getData();
        if(cloudEventData != null ) {
            // rebuild the context
            SecurityContextHolder.setContext(getContextFromCloudEvent(cloudEvent));
            try {
                return Optional.ofNullable(objectMapper.readValue(cloudEventData.toBytes(), clazz));

            } catch (Exception e) {
                log.error("Error deserializing CloudEvent data for eventId: [{}], class: [{}], error: {}",
                        cloudEvent.getId(),
                        clazz,
                        e.getMessage());
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> void withEventPayload(CloudEvent cloudEvent, Class<T> clazz, Consumer<T> consumer) {
        log.info("CloudEvent received for class: [{}], eventId: [{}]",
                clazz.getName(),
                cloudEvent.getId());

        try {
            CloudEventData cloudEventData = cloudEvent.getData();
            if (cloudEventData != null) {
                // initialize the security context from the CloudEvent
                SecurityContextHolder.setContext(getContextFromCloudEvent(cloudEvent));
                // Deserialize the payload
                final Optional<T> payloadOptional = Optional.ofNullable(objectMapper.readValue(
                        cloudEventData.toBytes(),
                        clazz));
                // If the payload is present, consume it
                if (payloadOptional.isPresent()) {
                    consumer.accept(payloadOptional.get());
                } else {
                    log.warn("Received null payload for eventId: [{}], class: [{}]", cloudEvent.getId(), clazz);
                }
            } else {
                log.warn("CloudEvent data is null for eventId: [{}], skipping...", cloudEvent.getId());
            }
        } catch (Exception e) {
            log.error("Failed to parse event payload: {}", e.getMessage(), e);
        } finally {
            // Clear the security context after processing the event
            SecurityContextHolder.clear();
        }
    }

    private WMPContext getContextFromCloudEvent(CloudEvent cloudEvent) {
        return WMPContext.builder()
                .email((String) cloudEvent.getExtension("email"))
                .userId((String) cloudEvent.getExtension("userid"))
                .bankCode((String) cloudEvent.getExtension("bankcode"))
                .correlationId((String) cloudEvent.getExtension("correlationid"))
                .build();
    }

}
