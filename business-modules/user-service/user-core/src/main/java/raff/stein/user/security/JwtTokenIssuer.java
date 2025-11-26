package raff.stein.user.security;

import io.jsonwebtoken.Jwts;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.*;

/**
 * Issues signed JWT tokens using an RSA private key.
 */
@Component
public class JwtTokenIssuer {

    @Value("${security.jwt.private-key-path:classpath:keys/private_key.pem}")
    private Resource privateKeyResource;

    @Value("${security.jwt.issuer:user-service}")
    private String issuer;

    @Getter
    @Value("${security.jwt.expiration-seconds:3600}")
    private long expirationSeconds;

    @Value("${security.jwt.password-setup-expiration-seconds:900}") // 15 minutes default
    private long passwordSetupExpirationSeconds;

    private RSAPrivateKey loadPrivateKey() {
        try (InputStream is = privateKeyResource.getInputStream()) {
            String keyPem = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(keyPem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            PrivateKey pk = KeyFactory.getInstance("RSA").generatePrivate(spec);
            return (RSAPrivateKey) pk;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read private key", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load private key", e);
        }
    }

    public String issuePasswordSetupToken(String userId, String email) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("email", email);
        extra.put("purpose", "PASSWORD_SETUP");
        // no roles for this token
        return buildJwt(userId, extra, passwordSetupExpirationSeconds);
    }

    public String issueToken(String subject, String email, List<String> roles, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>();
        if (extraClaims != null) {
            claims.putAll(extraClaims);
        }
        if (email != null) {
            claims.put("email", email);
        }
        if (roles != null) {
            claims.put("roles", roles);
        }
        return buildJwt(subject, claims, expirationSeconds);
    }

    private String buildJwt(String subject, Map<String, Object> claims, long ttlSeconds) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);
        RSAPrivateKey key = loadPrivateKey();

        return Jwts.builder()
                .subject(subject)
                .claims(claims == null ? Map.of() : claims)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key, Jwts.SIG.RS256)
                .compact();
    }

}
