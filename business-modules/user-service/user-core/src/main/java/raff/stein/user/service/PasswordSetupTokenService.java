package raff.stein.user.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.unauthorized.JwtTokenException;
import raff.stein.platformcore.security.jwt.TokenRevocationService;
import raff.stein.platformcore.security.jwt.WmpJwtClaims;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Validates and consumes password setup tokens to prevent reuse.
 * Mirrors the checks done by the PasswordConfirmJwtFilter, and additionally consumes the JTI in the shared token
 * store, so a setup link works once across all instances and restarts.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class PasswordSetupTokenService {

    @Value("${security.jwt.public-key-path:classpath:keys/public_key.pem}")
    private Resource publicKeyResource;

    private final TokenRevocationService tokenRevocationService;
    private final AtomicReference<RSAPublicKey> publicKeyRef = new AtomicReference<>();

    /**
     * Validates the token purpose/exp/signature and consumes its JTI so it cannot be reused.
     * Returns the subject (user identifier) or throws a 401 {@link JwtTokenException} on an invalid token.
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
            throw invalidToken();
        }
    }

    private void validatePurpose(Claims claims) {
        Object purpose = claims.get(WmpJwtClaims.PURPOSE);
        if (purpose == null || !WmpJwtClaims.PURPOSE_PASSWORD_SETUP.equals(purpose.toString())) {
            throw invalidToken();
        }
    }

    private long validateExp(Claims claims, long now) {
        Date expDate = claims.getExpiration();
        if (expDate == null) {
            throw invalidToken();
        }
        long exp = expDate.toInstant().getEpochSecond();
        if (now >= exp) {
            throw invalidToken();
        }
        return exp;
    }

    private void validateNbf(Claims claims, long now) {
        Date nbfDate = claims.getNotBefore();
        if (nbfDate != null) {
            long nbf = nbfDate.toInstant().getEpochSecond();
            if (now < nbf) {
                throw invalidToken();
            }
        }
    }

    private void consumeJti(Claims claims, long exp) {
        String jti = claims.getId();
        if (jti == null || jti.isBlank()) {
            throw invalidToken();
        }
        // the store entry expires with the token, so no cleanup is needed; a store outage fails closed
        boolean firstUse = tokenRevocationService.consumeOnce(jti, exp);
        if (!firstUse) {
            throw invalidToken();
        }
    }

    // one code for every cause: the client can only ask for a new link, and the cause isn't disclosed
    private static JwtTokenException invalidToken() {
        return new JwtTokenException(
                ErrorCode.PASSWORD_SETUP_TOKEN_INVALID,
                ErrorCode.PASSWORD_SETUP_TOKEN_INVALID.getMessageTemplate());
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
