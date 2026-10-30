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

            Long expEpoch = Optional.ofNullable(claims.getExpiration())
                    .map(d -> d.toInstant().getEpochSecond())
                    .orElse(null);

            WMPContext context = WMPContext.builder()
                    .userId(claims.get(WmpJwtClaims.USER_ID, String.class))
                    .email(claims.get(WmpJwtClaims.EMAIL, String.class))
                    .roles(extractRoles(claims))
                    .tenantId(claims.get(WmpJwtClaims.TENANT_ID, String.class))
                    .branchCode(claims.get(WmpJwtClaims.BRANCH_CODE, String.class))
                    .correlationId(correlationId)
                    .rawToken(token)
                    .jti(claims.getId())
                    .tokenExpEpochSeconds(expEpoch)
                    .build();
            return Optional.of(context);
        } catch (JwtException e) {
            log.error(e.getMessage());
            return Optional.empty();
        }
    }

    private Set<String> extractRoles(Claims claims) {
        Object raw = claims.get(WmpJwtClaims.ROLES);
        switch (raw) {
            case null -> {
                return Collections.emptySet();
            }
            case Collection<?> objects -> {
                return objects.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
            }
            case String str -> {
                if (str.isBlank()) {
                    return Collections.emptySet();
                }
                return Arrays.stream(str.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
            }
            default -> {
            }
        }
        log.warn("Unexpected type for roles claim: {}", raw.getClass().getName());
        return Collections.emptySet();
    }
}
