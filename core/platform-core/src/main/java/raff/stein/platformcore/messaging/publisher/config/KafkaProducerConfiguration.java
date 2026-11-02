package raff.stein.platformcore.messaging.publisher.config;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.message.Encoding;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.cloudevents.kafka.CloudEventSerializer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import raff.stein.platformcore.messaging.configuration.kafka.KafkaConfiguration;

import java.util.Map;
import java.util.UUID;

@Configuration
@Profile("!integration-test")
@RequiredArgsConstructor
public class KafkaProducerConfiguration {

    private final KafkaConfiguration kafkaConfiguration;

    @Bean
    public Producer<String, CloudEvent> kafkaCloudEventProducer() {
        Map<String, Object> props = kafkaConfiguration.getBasicBrokerProperties();
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "wmp-cloud-event-producer-" + UUID.randomUUID());
        props.put(CloudEventSerializer.ENCODING_CONFIG, Encoding.STRUCTURED);
        props.put(CloudEventSerializer.EVENT_FORMAT_CONFIG, EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE));
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CloudEventSerializer.class);
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1000000); // 1 MB
        // the outbox relay retries, so the broker must not store the same record twice when a send is retried
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        // send() waits this long for topic metadata when the broker is unreachable (60s by default)
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        return new DefaultKafkaProducerFactory<String, CloudEvent>(props).createProducer();
    }
}
