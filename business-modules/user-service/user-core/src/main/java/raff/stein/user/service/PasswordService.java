package raff.stein.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import raff.stein.user.validator.PasswordPolicyValidator;

import java.util.List;

/**
 * Service responsible for password related operations
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;

    /**
     * Validates password against policy and returns a list of violations. Empty list means valid.
     */
    public List<String> validatePolicy(String plainPassword, String usernameOrEmail) {
        return passwordPolicyValidator.validate(plainPassword, usernameOrEmail);
    }

    /**
     * Hashes the supplied plain password using the configured PasswordEncoder.
     */
    public String encode(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

}
