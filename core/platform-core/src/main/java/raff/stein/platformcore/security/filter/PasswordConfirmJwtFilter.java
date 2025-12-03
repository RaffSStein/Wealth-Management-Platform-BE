package raff.stein.platformcore.security.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Validates the JWT used for password setup confirmation:
 * - checks "purpose" == "PASSWORD_SETUP"
 * - checks "exp" claim not expired
 * - verifies the JWT signature using the configured public key
 */
@Component
public class PasswordConfirmJwtFilter extends OncePerRequestFilter {

    private static final String TARGET_PATH = "/auth/password/setup";
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${security.jwt.public-key-path:classpath:keys/public_key.pem}")
    private org.springframework.core.io.Resource publicKeyResource;

    private RSAPublicKey publicKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // apply only to POST /auth/password/confirm (adjust as needed)
        return !(TARGET_PATH.equals(request.getServletPath()) && "POST".equalsIgnoreCase(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws IOException, ServletException {

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            sendUnauthorized(response, "Missing Authorization Bearer token");
            return;
        }

        String token = auth.substring(7).trim();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(loadPublicKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Object purpose = claims.get("purpose");
            if (purpose == null || !"PASSWORD_SETUP".equals(purpose.toString())) {
                sendUnauthorized(response, "Invalid token purpose");
                return;
            }

            Object expObj = claims.get("exp");
            if (expObj == null) {
                sendUnauthorized(response, "Missing exp claim");
                return;
            }
            long exp;
            if (expObj instanceof Number number) {
                exp = number.longValue();
            } else {
                exp = Long.parseLong(expObj.toString());
            }
            long now = Instant.now().getEpochSecond();
            if (now >= exp) {
                sendUnauthorized(response, "Token expired");
                return;
            }

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            sendUnauthorized(response, "Token expired");
        } catch (SignatureException e) {
            sendUnauthorized(response, "Invalid token signature");
        } catch (Exception e) {
            sendUnauthorized(response, "Invalid token payload");
        }
    }

    private RSAPublicKey loadPublicKey() {
        if (publicKey != null) {
            return publicKey;
        }
        try (InputStream is = publicKeyResource.getInputStream()) {
            String keyPem = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(keyPem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            PublicKey pk = KeyFactory.getInstance("RSA").generatePublic(spec);
            this.publicKey = (RSAPublicKey) pk;
            return this.publicKey;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load public key for JWT validation", e);
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = mapper.writeValueAsString(Map.of("error", message));
        response.getWriter().write(body);
    }
}
