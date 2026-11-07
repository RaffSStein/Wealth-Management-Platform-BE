package raff.stein.platformcore.security.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.unauthorized.JwtTokenException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecentAuthenticationGuardTest {

    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");
    private final RecentAuthenticationGuard guard = new RecentAuthenticationGuard(Clock.fixed(NOW, ZoneOffset.UTC));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    void shouldAcceptRecentLogin() {
        givenTokenIssued(NOW.minusSeconds(599));

        assertDoesNotThrow(() -> guard.requireAuthenticatedWithin(Duration.ofMinutes(10)));
    }

    @Test
    void shouldRequireReauthenticationForOldLogin() {
        givenTokenIssued(NOW.minusSeconds(601));

        final Duration maxAge = Duration.ofMinutes(10);
        final JwtTokenException exception = assertThrows(JwtTokenException.class,
                () -> guard.requireAuthenticatedWithin(maxAge));
        assertEquals(ErrorCode.REAUTHENTICATION_REQUIRED, exception.getErrorCode());
    }

    @Test
    void shouldRequireReauthenticationForUndatedToken() {
        SecurityContextHolder.setContext(WMPContext.builder().email("admin@bank.test").build());

        final Duration maxAge = Duration.ofMinutes(10);

        assertThrows(JwtTokenException.class, () -> guard.requireAuthenticatedWithin(maxAge));
    }

    private static void givenTokenIssued(Instant issuedAt) {
        SecurityContextHolder.setContext(WMPContext.builder()
                .email("admin@bank.test")
                .tokenIssuedAtEpochSeconds(issuedAt.getEpochSecond())
                .build());
    }
}
