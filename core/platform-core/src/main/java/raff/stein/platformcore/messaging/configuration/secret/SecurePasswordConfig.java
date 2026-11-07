package raff.stein.platformcore.messaging.configuration.secret;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Configuration
@ConfigurationProperties(prefix = "secure.password")
// This class is used to configure secure password properties.
public class SecurePasswordConfig {

    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Getter
    private String algorithm = AES_GCM;

    @Setter
    private String keyAlgorithm = AES;

    private SecretKey aesKey;

    @PostConstruct
    public SecurePasswordConfig initialize() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        // Fail at startup, as before, if the cipher is not available
        Cipher.getInstance(algorithm);
        final KeyGenerator keyGenerator = KeyGenerator.getInstance(keyAlgorithm);
        aesKey = keyGenerator.generateKey();
        return this;
    }

    public SealedObject getSecurePassword(String password) throws IllegalBlockSizeException, IOException {
        // GCM must never reuse an IV with the same key, so every seal gets its own cipher and random IV
        final byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);
        try {
            final Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new SealedObject(password, cipher);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new IllegalStateException("Cannot initialize cipher " + algorithm, e);
        }
    }

    public String getPlainValue(SealedObject securePassword)
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeyException {
        return (String) securePassword.getObject(aesKey);
    }
}
