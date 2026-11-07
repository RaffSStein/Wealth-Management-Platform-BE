package raff.stein.user.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyValidatorTest {

    private static final String UPPER = "Password must contain at least one uppercase letter";
    private static final String LOWER = "Password must contain at least one lowercase letter";
    private static final String DIGIT = "Password must contain at least one digit";
    private static final String SPECIAL = "Password must contain at least one special character";
    private static final String WHITESPACE = "Password must not contain whitespace";
    private static final String TOO_SHORT = "Password must be at least 8 characters long";
    private static final String USER_RELATED = "Password must not contain parts of the username/email";

    private final PasswordPolicyValidator validator = new PasswordPolicyValidator();

    @Test
    void shouldAcceptCompliantPassword() {
        assertEquals(List.of(), validator.validate("Str0ng!Pass", "mario.rossi@bank.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "_", "-", "+", "=", "[", "]", "{", "}",
            ";", ":", "'", "\"", "|", "\\", ",", "<", ".", ">", "/", "?"})
    void shouldAcceptEverySupportedSpecialCharacter(String special) {
        assertEquals(List.of(), validator.validate("Abcdef1" + special, null));
    }

    @Test
    void shouldRejectBlankPassword() {
        assertEquals(List.of("Password must not be blank"), validator.validate("   ", null));
        assertEquals(List.of("Password must not be blank"), validator.validate(null, null));
    }

    @Test
    void shouldReportEachMissingCharacterClass() {
        assertEquals(List.of(UPPER), validator.validate("str0ng!pass", null));
        assertEquals(List.of(LOWER), validator.validate("STR0NG!PASS", null));
        assertEquals(List.of(DIGIT), validator.validate("Strong!Pass", null));
        assertEquals(List.of(SPECIAL), validator.validate("Str0ngPass", null));
        assertEquals(List.of(SPECIAL), validator.validate("Str0ng€Pass", null));
    }

    @Test
    void shouldReportAllViolationsInOrder() {
        assertEquals(List.of(TOO_SHORT, UPPER, DIGIT, SPECIAL), validator.validate("abc", null));
    }

    @Test
    void shouldRejectSpaceAndTab() {
        assertEquals(List.of(WHITESPACE), validator.validate("Str0ng! Pass", null));
        assertEquals(List.of(WHITESPACE), validator.validate("Str0ng!\tPass", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Str0ng!\nPass", "Str0ng!\rPass", "Str0ng!\r\nPass", "Str0ng!\u0085Pass",
            "Str0ng! Pass", "Str0ng! Pass", "A\nb\nc\n1!xyz"})
    void shouldRejectPasswordsWithLineTerminators(String password) {
        // Rejection is the contract here; the exact messages are not
        assertFalse(validator.validate(password, null).isEmpty());
    }

    @Test
    void shouldRejectPasswordContainingEmailLocalPart() {
        assertEquals(List.of(USER_RELATED), validator.validate("Xmario.rossi1!", "Mario.Rossi@bank.com"));
        assertEquals(List.of(USER_RELATED), validator.validate("Xmario1!aa", "mario"));
        assertTrue(validator.validate("Str0ng!Pass", "@bank.com").isEmpty());
    }
}
