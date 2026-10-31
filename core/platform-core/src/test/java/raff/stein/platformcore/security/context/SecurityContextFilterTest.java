package raff.stein.platformcore.security.context;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import raff.stein.platformcore.exception.types.unavailable.TokenStoreUnavailableException;
import raff.stein.platformcore.security.jwt.JwtProperties;
import raff.stein.platformcore.security.jwt.JwtTokenParser;
import raff.stein.platformcore.security.jwt.TokenRevocationService;
import raff.stein.platformcore.security.jwt.WmpJwtClaims;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityContextFilterTest {

    private static final String TOKEN = "signed-token";

    private JwtTokenParser tokenParser;
    private TokenRevocationService tokenRevocationService;
    private SecurityContextFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        tokenParser = mock(JwtTokenParser.class);
        tokenRevocationService = mock(TokenRevocationService.class);
        filter = new SecurityContextFilter(tokenParser, new JwtProperties(), tokenRevocationService);
        chain = mock(FilterChain.class);
    }

    @Test
    void shouldRejectPasswordSetupTokenOnOtherEndpoints() throws Exception {
        givenToken(context(WmpJwtClaims.PURPOSE_PASSWORD_SETUP));
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/customer/42"), response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldAcceptPasswordSetupTokenOnPasswordSetupEndpoint() throws Exception {
        givenToken(context(WmpJwtClaims.PURPOSE_PASSWORD_SETUP));
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("POST", "/auth/password/setup"), response, chain);

        verify(chain).doFilter(any(), eq(response));
    }

    @Test
    void shouldRejectTokenWithUnknownPurposeEverywhere() throws Exception {
        givenToken(context("SOMETHING_ELSE"));
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("POST", "/auth/password/setup"), response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldSetContextForSessionTokenAndClearItAfterwards() throws Exception {
        givenToken(context(null));
        final AtomicReference<WMPContext> contextDuringRequest = new AtomicReference<>();
        doAnswer(invocation -> {
            contextDuringRequest.set(SecurityContextHolder.getContext());
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request("GET", "/customer/42"), new MockHttpServletResponse(), chain);

        assertEquals("BANK_A", contextDuringRequest.get().getTenantId());
        assertNull(SecurityContextHolder.getContext());
    }

    @Test
    void shouldRejectRevokedToken() throws Exception {
        givenToken(context(null));
        when(tokenRevocationService.isRevoked("jti-1")).thenReturn(true);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/customer/42"), response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectTokenRevokedForTheWholeUser() throws Exception {
        givenToken(context(null));
        when(tokenRevocationService.isRevokedForUser("user-1", 1_000L)).thenReturn(true);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/customer/42"), response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldFailClosedWhenTokenStoreIsUnavailable() throws Exception {
        givenToken(context(null));
        when(tokenRevocationService.isRevoked("jti-1"))
                .thenThrow(new TokenStoreUnavailableException("check revoked", new RuntimeException("connection refused")));
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/customer/42"), response, chain);

        assertEquals(503, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
        assertNull(SecurityContextHolder.getContext());
    }

    private void givenToken(WMPContext context) {
        when(tokenParser.parseTokenAndBuildContext(eq(TOKEN), any())).thenReturn(Optional.of(context));
    }

    private static WMPContext context(String purpose) {
        return WMPContext.builder()
                .userId("user-1")
                .email("user@bank.test")
                .tenantId("BANK_A")
                .tokenIssuedAtEpochSeconds(1_000L)
                .jti("jti-1")
                .purpose(purpose)
                .build();
    }

    private static MockHttpServletRequest request(String method, String servletPath) {
        final MockHttpServletRequest request = new MockHttpServletRequest(method, servletPath);
        request.setServletPath(servletPath);
        request.addHeader("Authorization", "Bearer " + TOKEN);
        return request;
    }
}
