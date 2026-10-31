package raff.stein.user.service;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;
import raff.stein.platformcore.exception.types.unauthorized.JwtTokenException;
import raff.stein.platformcore.exception.types.unavailable.TokenStoreUnavailableException;
import raff.stein.platformcore.security.jwt.TokenRevocationService;
import raff.stein.platformcore.security.jwt.WmpJwtClaims;
import raff.stein.platformcore.security.jwt.revocation.InMemoryTokenStore;
import raff.stein.platformcore.security.jwt.revocation.TokenStoreProperties;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PasswordSetupTokenServiceTest {

    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    void shouldAcceptSetupTokenOnlyOnce() {
        final PasswordSetupTokenService service = service(new TokenRevocationService(new InMemoryTokenStore(), new TokenStoreProperties()));
        final String token = setupToken(WmpJwtClaims.PURPOSE_PASSWORD_SETUP);

        assertEquals("user-1", service.validateAndConsume(token));
        assertThrows(JwtTokenException.class, () -> service.validateAndConsume(token));
    }

    @Test
    void shouldRejectTokenWithoutSetupPurpose() {
        final PasswordSetupTokenService service = service(new TokenRevocationService(new InMemoryTokenStore(), new TokenStoreProperties()));

        assertThrows(JwtTokenException.class, () -> service.validateAndConsume(setupToken("OTHER")));
    }

    @Test
    void shouldRefuseSetupWhenTokenStoreIsUnavailable() {
        final TokenRevocationService revocationService = mock(TokenRevocationService.class);
        when(revocationService.consumeOnce(anyString(), anyLong()))
                .thenThrow(new TokenStoreUnavailableException("mark setup-used", new RuntimeException("down")));
        final PasswordSetupTokenService service = service(revocationService);

        assertThrows(TokenStoreUnavailableException.class,
                () -> service.validateAndConsume(setupToken(WmpJwtClaims.PURPOSE_PASSWORD_SETUP)));
    }

    private PasswordSetupTokenService service(TokenRevocationService revocationService) {
        final PasswordSetupTokenService service = new PasswordSetupTokenService(revocationService);
        final String pem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
        ReflectionTestUtils.setField(service, "publicKeyResource", new ByteArrayResource(pem.getBytes(StandardCharsets.UTF_8)));
        return service;
    }

    private String setupToken(String purpose) {
        final Instant now = Instant.now();
        return Jwts.builder()
                .subject("user-1")
                .id(UUID.randomUUID().toString())
                .claim(WmpJwtClaims.PURPOSE, purpose)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .signWith(keyPair.getPrivate())
                .compact();
    }
}
