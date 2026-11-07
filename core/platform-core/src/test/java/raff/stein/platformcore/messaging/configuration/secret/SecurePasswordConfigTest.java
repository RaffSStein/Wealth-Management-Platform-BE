package raff.stein.platformcore.messaging.configuration.secret;

import org.junit.jupiter.api.Test;
import raff.stein.platformcore.messaging.configuration.kafka.KafkaBrokerConfiguration;

import javax.crypto.SealedObject;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SecurePasswordConfigTest {

    private static final String JAAS_TEMPLATE =
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";";

    @Test
    void shouldReturnOriginalValueAfterSealing() throws Exception {
        final SecurePasswordConfig config = new SecurePasswordConfig().initialize();

        final SealedObject sealed = config.getSecurePassword("s3cr3t-P4ss!");

        assertEquals("s3cr3t-P4ss!", config.getPlainValue(sealed));
    }

    @Test
    void shouldUnsealEverySealedValueIndependently() throws Exception {
        final SecurePasswordConfig config = new SecurePasswordConfig().initialize();

        final SealedObject first = config.getSecurePassword("first");
        final SealedObject second = config.getSecurePassword("second");

        assertEquals("first", config.getPlainValue(first));
        assertEquals("second", config.getPlainValue(second));
    }

    @Test
    void shouldSealWithAuthenticatedCipherAndFreshIv() throws Exception {
        final SecurePasswordConfig config = new SecurePasswordConfig().initialize();

        final SealedObject first = config.getSecurePassword("same");
        final SealedObject second = config.getSecurePassword("same");

        assertEquals("AES/GCM/NoPadding", first.getAlgorithm());
        assertNotEquals(serializedFormOf(first), serializedFormOf(second));
    }

    @Test
    void shouldSealNullValue() throws Exception {
        final SecurePasswordConfig config = new SecurePasswordConfig().initialize();

        assertNull(config.getPlainValue(config.getSecurePassword(null)));
    }

    @Test
    void shouldApplyBrokerCredentialsToConnectionProperty() {
        final KafkaBrokerConfiguration brokerConfiguration = new KafkaBrokerConfiguration();
        brokerConfiguration.setUser("kafka-user");
        brokerConfiguration.setPwd("kafka-pwd");

        final SecretManager secretManager = new SecretManager(brokerConfiguration);

        assertEquals(
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kafka-user\" password=\"kafka-pwd\";",
                secretManager.applyBrokerSecrets(JAAS_TEMPLATE));
    }

    @Test
    void shouldApplyNullPasswordWhenBrokerPasswordIsMissing() {
        final KafkaBrokerConfiguration brokerConfiguration = new KafkaBrokerConfiguration();
        brokerConfiguration.setUser("kafka-user");

        final SecretManager secretManager = new SecretManager(brokerConfiguration);

        assertEquals(
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kafka-user\" password=\"null\";",
                secretManager.applyBrokerSecrets(JAAS_TEMPLATE));
    }

    // Serialized form holds ciphertext and IV: with a deterministic mode (ECB) two seals of the same value are identical
    private static String serializedFormOf(SealedObject sealed) throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(sealed);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
}
