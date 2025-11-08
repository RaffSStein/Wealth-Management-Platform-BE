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
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    public String issueToken(String subject, String email, List<String> roles, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationSeconds);
        RSAPrivateKey key = loadPrivateKey();
        return Jwts.builder()
                .subject(subject)
                .claim("email", email)
                .claim("roles", roles)
                .claims(extraClaims == null ? Map.of() : extraClaims)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key, Jwts.SIG.RS256)
                .compact();
    }

}
