package raff.stein.platformcore.security.jwt;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.platformcore.security.context.WMPContext;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenParserTest {

    private KeyPair keyPair;
    private JwtTokenParser parser;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        parser = new JwtTokenParser(keyPair.getPublic());
    }

    @Test
    void shouldMapTenantAndBranchClaimsToContext() {
        final String token = Jwts.builder()
                .claim(WmpJwtClaims.USER_ID, "user-1")
                .claim(WmpJwtClaims.EMAIL, "user@bank.test")
                .claim(WmpJwtClaims.ROLES, List.of("ADVISOR"))
                .claim(WmpJwtClaims.TENANT_ID, "BANK_A")
                .claim(WmpJwtClaims.BRANCH_CODE, "BRANCH_01")
                .signWith(keyPair.getPrivate())
                .compact();

        final Optional<WMPContext> context = parser.parseTokenAndBuildContext(token, "corr-1");

        assertTrue(context.isPresent());
        assertEquals("BANK_A", context.get().getTenantId());
        assertEquals("BRANCH_01", context.get().getBranchCode());
        assertEquals(Set.of("ADVISOR"), context.get().getRoles());
        assertEquals("corr-1", context.get().getCorrelationId());
    }

    @Test
    void shouldLeaveTenantEmptyWhenClaimIsMissing() {
        final String token = Jwts.builder()
                .claim(WmpJwtClaims.EMAIL, "customer@bank.test")
                .signWith(keyPair.getPrivate())
                .compact();

        final Optional<WMPContext> context = parser.parseTokenAndBuildContext(token, null);

        assertTrue(context.isPresent());
        assertNull(context.get().getTenantId());
        assertNull(context.get().getBranchCode());
    }

    @Test
    void shouldRejectTokenSignedWithAnotherKey() throws NoSuchAlgorithmException {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        final String token = Jwts.builder()
                .claim(WmpJwtClaims.TENANT_ID, "BANK_A")
                .signWith(generator.generateKeyPair().getPrivate())
                .compact();

        assertTrue(parser.parseTokenAndBuildContext(token, null).isEmpty());
    }
}
