package raff.stein.platformcore.messaging.configuration.secret;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.messaging.configuration.kafka.KafkaAwsSecrets;
import raff.stein.platformcore.messaging.configuration.kafka.KafkaBrokerConfiguration;

import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Component
@Slf4j
public class SecretManager {

    public static final String NOT_SOLVED_VARIABLE = "$";
    private final SecurePasswordConfig securePasswordConfig = new SecurePasswordConfig();
    @Getter
    private Optional<KafkaAwsSecrets> kafkaAwsSecrets;
    //TODO: do the same for mongoDbAwsSecrets
//    @Getter
//    private Optional<MongoDbAwsSecrets> mongoDbAwsSecrets;

    public SecretManager(@NonNull KafkaBrokerConfiguration kafkaBrokerConfiguration) {
        try {
            securePasswordConfig.initialize();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        initBrokerAwsSecrets(kafkaBrokerConfiguration);
    }


    @RequiredArgsConstructor
    private final class SecretsInjector {
        // This class is used to inject secrets into the application context.
        // It can be extended to implement specific secret management logic.
        private final String property;

        String applySecret(AwsSecrets secrets) {
            // Logic to apply the secret, e.g., setting it in the application context
            if (secrets == null) {
                return applyEmptySecret();
            }
            try {
                return String.format(property, secrets.getUser(), securePasswordConfig.getPlainValue(secrets.getPassword()));
            } catch (Exception e) {
                log.error("Error in decrypting secret config: [{}]", secrets.getClass().getName(), e);
                return applyEmptySecret();
            }
        }

        String applyEmptySecret() {
            // Logic to handle empty secrets, e.g., returning a default value
            log.warn("No secret found for property: {}", property);
            return String.format(property, "", ""); // Return an empty string or a default value
        }
    }


    /**
     * Initializes the Kafka AWS secrets based on the provided Kafka broker configuration.
     *
     * @param kafkaBrokerConfiguration the Kafka broker configuration containing AWS secrets
     */
    private void initBrokerAwsSecrets(@NonNull KafkaBrokerConfiguration kafkaBrokerConfiguration) {
        try {
            kafkaAwsSecrets = Optional.of(
                    KafkaAwsSecrets.builder()
                            .server(kafkaBrokerConfiguration.getConnectionUri())
                            .secure(kafkaBrokerConfiguration.isSecureConnection())
                            .user(kafkaBrokerConfiguration.getUser())
                            .password(securePasswordConfig.getSecurePassword(kafkaBrokerConfiguration.getPwd()))
                            .build());
        } catch (Exception e) {
            log.error("Error on encrypting Kafka AWS secrets: [{}]", kafkaBrokerConfiguration.getClass().getName(), e);
        }
    }

    /**
     * Applies credentials to Kafka-property by {@link String#format(String, Object...)}.
     *
     * @param connectionProperty the connection property to apply secrets to
     * </br> example: org.apache.kafka.common.security.plain.PlainLoginModule required username="%s" password="%s";
     * @return the connection property with applied secrets, or null if no secrets are available
     * </br> example: org.apache.kafka.common.security.plain.PlainLoginModule required username="user" password="password";
     */
    public String applyBrokerSecrets(String connectionProperty) {
        return kafkaAwsSecrets.map(
                new SecretsInjector(connectionProperty)::applySecret)
                .orElse(null);
    }
}
