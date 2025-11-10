package raff.stein.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Service responsible for password related operations:
 * - Temporary password generation
 * - Hashing (encoding) of passwords
 * - Masking for safe logging
 * <p>
 * NOTE: Plain passwords must never be logged in full nor returned in API responses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

    private final PasswordEncoder passwordEncoder;

    private static final String PASSWORD_ALLOWED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*_-";
    private static final int DEFAULT_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a cryptographically strong temporary password.
     */
    public String generateTemporaryPassword() {
        return IntStream.range(0, DEFAULT_LENGTH)
                .map(i -> PASSWORD_ALLOWED.charAt(SECURE_RANDOM.nextInt(PASSWORD_ALLOWED.length())))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    /**
     * Hashes the supplied plain password using the configured PasswordEncoder.
     */
    public String encode(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

    /**
     * Returns a masked representation of the password suitable for logs.
     */
    public String mask(String password) {
        if (password == null || password.length() <= 4) return "****"; // fallback
        return password.substring(0, 2) + "****" + password.substring(password.length() - 2);
    }

}

