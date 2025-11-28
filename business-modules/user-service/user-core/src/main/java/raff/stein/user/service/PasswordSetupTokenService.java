package raff.stein.user.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Validates and consumes password setup tokens to prevent reuse.
 * Mirrors the checks done by the PasswordConfirmJwtFilter, and additionally tracks JTI consumption.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class PasswordSetupTokenService {

    @Value("${security.jwt.public-key-path:classpath:keys/public_key.pem}")
    private Resource publicKeyResource;

    private final AtomicReference<RSAPublicKey> publicKeyRef = new AtomicReference<>();
    // Track consumed JTIs with their expiration to allow future cleanup
    private final Map<String, Long> consumedJtis = new ConcurrentHashMap<>();

    /**
     * Validates the token purpose/exp/signature and consumes its JTI so it cannot be reused.
     * Returns the subject (user identifier) or throws IllegalArgumentException on invalid token.
     */
    public String validateAndConsume(String jwt) {
        Claims claims = parseClaims(jwt);
        long now = Instant.now().getEpochSecond();
        validatePurpose(claims);
        long exp = validateExp(claims, now);
        validateNbf(claims, now);
        consumeJti(claims, exp);
        return claims.getSubject();
    }

    private Claims parseClaims(String jwt) {
        try {
            return Jwts.parser()
                    .verifyWith(loadPublicKey())
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
        } catch (ExpiredJwtException | SignatureException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid token");
        }
    }

    /**
     * Optional: removes expired JTI entries. Should be called periodically by a scheduler.
     */
    public void cleanupExpiredJtis() {
        long now = Instant.now().getEpochSecond();
        consumedJtis.entrySet().removeIf(e -> e.getValue() <= now);
    }

    private void validatePurpose(Claims claims) {
        Object purpose = claims.get("purpose");
        if (purpose == null || !"PASSWORD_SETUP".equals(purpose.toString())) {
            throw new IllegalArgumentException("Invalid token");
        }
    }

    private long validateExp(Claims claims, long now) {
        Date expDate = claims.getExpiration();
        if (expDate == null) {
            throw new IllegalArgumentException("Invalid token");
        }
        long exp = expDate.toInstant().getEpochSecond();
        if (now >= exp) {
            throw new IllegalArgumentException("Invalid token");
        }
        return exp;
    }

    private void validateNbf(Claims claims, long now) {
        Date nbfDate = claims.getNotBefore();
        if (nbfDate != null) {
            long nbf = nbfDate.toInstant().getEpochSecond();
            if (now < nbf) {
                throw new IllegalArgumentException("Invalid token");
            }
        }
    }

    private void consumeJti(Claims claims, long exp) {
        String jti = claims.getId();
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("Invalid token");
        }
        // TODO: use redis or database for distributed environments, also add a scheduler to cleanup expired entries.
        boolean firstUse = consumedJtis.putIfAbsent(jti, exp) == null;
        if (!firstUse) {
            throw new IllegalArgumentException("Invalid token");
        }
    }

    private RSAPublicKey loadPublicKey() {
        RSAPublicKey existing = publicKeyRef.get();
        if (existing != null) {
            return existing;
        }
        try (InputStream is = publicKeyResource.getInputStream()) {
            String keyPem = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(keyPem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            PublicKey pk = KeyFactory.getInstance("RSA").generatePublic(spec);
            RSAPublicKey rsa = (RSAPublicKey) pk;
            publicKeyRef.compareAndSet(null, rsa);
            return rsa;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load public key for JWT validation", e);
        }
    }
}
