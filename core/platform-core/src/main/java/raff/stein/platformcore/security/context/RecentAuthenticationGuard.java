package raff.stein.platformcore.security.context;

import org.springframework.stereotype.Component;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.unauthorized.JwtTokenException;

import java.time.Clock;
import java.time.Duration;

/**
 * Sensitive operations (granting access, approving requests) require a recent login, so a stolen long-lived token
 * can't be used for them. Fails with 401 REAUTHENTICATION_REQUIRED: the client asks the user to sign in again.
 */
@Component
public class RecentAuthenticationGuard {

    private final Clock clock;

    public RecentAuthenticationGuard() {
        this(Clock.systemUTC());
    }

    RecentAuthenticationGuard(Clock clock) {
        this.clock = clock;
    }

    public void requireAuthenticatedWithin(Duration maxAge) {
        final WMPContext context = SecurityContextHolder.getContext();
        final Long issuedAt = context == null ? null : context.getTokenIssuedAtEpochSeconds();
        final long now = clock.instant().getEpochSecond();
        if (issuedAt == null || now - issuedAt > maxAge.toSeconds()) {
            final String message = String.format(
                    ErrorCode.REAUTHENTICATION_REQUIRED.getMessageTemplate(), maxAge.toSeconds());
            throw new JwtTokenException(ErrorCode.REAUTHENTICATION_REQUIRED, message);
        }
    }
}
