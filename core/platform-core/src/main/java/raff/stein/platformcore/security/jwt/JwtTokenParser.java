package raff.stein.platformcore.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import raff.stein.platformcore.security.context.WMPContext;

import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Responsible for parsing and validating the JWT token.
 */
@Slf4j
public class JwtTokenParser {

    private final PublicKey publicKey;

    public JwtTokenParser(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    /**
     * Parses and validates a JWT using the provided public key.
     * @param token the JWT as a String
     * @return the parsed claims
     * @throws JwtException if the token is invalid or expired
     */
    public Optional<WMPContext> parseTokenAndBuildContext(String token, String correlationId) {
        try {
            Jws<Claims> claimsJws = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token);
            Claims claims = claimsJws.getPayload();

            WMPContext context = WMPContext.builder()
                    .userId(claims.get("userId", String.class))
                    .email(claims.get("email", String.class))
                    .roles(extractRoles(claims))
                    //TODO: probably not needed, as we can get it from the user-service
                    .bankCode(claims.get("bankCode", String.class))
                    .correlationId(correlationId)
                    .rawToken(token)
                    .build();
            return Optional.of(context);
        } catch (JwtException e) {
            log.error(e.getMessage());
            return Optional.empty();
        }
    }

    private Set<String> extractRoles(Claims claims) {
        Object raw = claims.get("roles");
        if (raw == null) {
            return Collections.emptySet();
        }
        if (raw instanceof Collection<?>) {
            return ((Collection<?>) raw).stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        }
        if (raw instanceof String str) {
            if (str.isBlank()) {
                return Collections.emptySet();
            }
            return Arrays.stream(str.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        }
        log.warn("Unexpected type for roles claim: {}", raw.getClass().getName());
        return Collections.emptySet();
    }
}
