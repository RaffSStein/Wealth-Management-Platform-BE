package raff.stein.user.validator;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates password strength according to platform policy.
 * Rules (default):
 * - Minimum length 8
 * - Must include at least 1 uppercase, 1 lowercase, 1 digit, and 1 special character
 * - Must not contain the username/email local-part
 * - Must not contain whitespace
 */
@Component
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    // Properly escaped character class for common specials
    private static final Pattern SPECIAL = Pattern.compile(".*[!@#$%^&*()_\\-+=\\[\\]{};:'\"|\\\\,<.>/?].*");
    private static final Pattern WHITESPACE = Pattern.compile(".*\\s.*");

    /**
     * Returns a list of violations. Empty list means valid.
     */
    public List<String> validate(String password, String usernameOrEmail) {
        List<String> violations = new ArrayList<>();
        if (password == null || password.isBlank()) {
            violations.add("Password must not be blank");
            return violations;
        }
        // Basic structure checks
        checkStructure(password, violations);
        // Check against username/email
        checkUserRelated(password, usernameOrEmail, violations);
        return violations;
    }

    private void checkStructure(String password, List<String> violations) {
        if (password.length() < MIN_LENGTH) {
            violations.add("Password must be at least " + MIN_LENGTH + " characters long");
        }
        if (!UPPER.matcher(password).matches()) {
            violations.add("Password must contain at least one uppercase letter");
        }
        if (!LOWER.matcher(password).matches()) {
            violations.add("Password must contain at least one lowercase letter");
        }
        if (!DIGIT.matcher(password).matches()) {
            violations.add("Password must contain at least one digit");
        }
        if (!SPECIAL.matcher(password).matches()) {
            violations.add("Password must contain at least one special character");
        }
        if (WHITESPACE.matcher(password).matches()) {
            violations.add("Password must not contain whitespace");
        }
    }

    private void checkUserRelated(String password, String usernameOrEmail, List<String> violations) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return;
        }
        String local = usernameOrEmail;
        int atIdx = usernameOrEmail.indexOf('@');
        if (atIdx > 0) {
            local = usernameOrEmail.substring(0, atIdx);
        }
        if (!local.isBlank()) {
            String lowerPass = password.toLowerCase();
            String lowerLocal = local.toLowerCase();
            if (lowerPass.contains(lowerLocal)) {
                violations.add("Password must not contain parts of the username/email");
            }
        }
    }
}
