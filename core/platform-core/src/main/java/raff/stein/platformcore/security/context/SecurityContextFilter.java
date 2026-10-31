package raff.stein.platformcore.security.context;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import raff.stein.platformcore.exception.types.unavailable.TokenStoreUnavailableException;
import raff.stein.platformcore.security.filter.PasswordConfirmJwtFilter;
import raff.stein.platformcore.security.jwt.JwtProperties;
import raff.stein.platformcore.security.jwt.JwtTokenParser;
import raff.stein.platformcore.security.jwt.TokenRevocationService;
import raff.stein.platformcore.security.jwt.WmpJwtClaims;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter that extracts the JWT from the request, parses it,
 * and stores the authenticated user in the custom WMP context.
 * It also sets the user information in the MDC for logging purposes.
 * <p>
 * This filter is non-blocking: if no token is present or token is invalid,
 * it does not fail the request. Authorization is handled by Spring Security downstream.
 */
@Slf4j
public class SecurityContextFilter implements Filter {

    private final JwtTokenParser tokenParser;
    private final JwtProperties jwtProperties;
    private final TokenRevocationService tokenRevocationService;

    public SecurityContextFilter(
            JwtTokenParser tokenParser,
            JwtProperties jwtProperties,
            TokenRevocationService tokenRevocationService) {
        this.tokenParser = tokenParser;
        this.jwtProperties = jwtProperties;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            String authHeader = httpRequest.getHeader(jwtProperties.getHeader());
            String correlationId = httpRequest.getHeader("X-Correlation-ID");

            if (isTokenPresent(authHeader)) {
                String token = extractToken(authHeader);
                Optional<WMPContext> wmpContextOptional = tokenParser.parseTokenAndBuildContext(token, correlationId);
                if (wmpContextOptional.isPresent()) {
                    WMPContext context = wmpContextOptional.get();
                    // Use jti from context to check revocation without reparsing/reflection
                    final boolean revoked;
                    try {
                        revoked = tokenRevocationService.isRevoked(context.getJti())
                                || tokenRevocationService.isRevokedForUser(
                                        context.getUserId(), context.getTokenIssuedAtEpochSeconds());
                    } catch (TokenStoreUnavailableException e) {
                        // fail closed: a revoked token must not slip through while the store is down
                        log.error("Token store unavailable, rejecting request [{} {}]",
                                httpRequest.getMethod(), httpRequest.getServletPath(), e);
                        httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                        return;
                    }
                    if (revoked) {
                        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return; // Stop chain for revoked token
                    }
                    // a single-purpose token (e.g. password setup) is signed like a session token:
                    // without this check every authenticated endpoint would accept it
                    if (isUsedOutsideItsEndpoint(context.getPurpose(), httpRequest)) {
                        log.warn("Rejected [{}] token used on [{} {}]",
                                context.getPurpose(), httpRequest.getMethod(), httpRequest.getServletPath());
                        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    SecurityContextHolder.setContext(context);
                    // Set MDC for logging
                    MDC.put("userId", context.getUserId());
                    MDC.put("email", context.getEmail());
                    MDC.put("tenantId", context.getTenantId());
                    MDC.put("branchCode", context.getBranchCode());
                    MDC.put("correlationId", context.getCorrelationId());
                }
            }
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clear();
            MDC.clear();
        }
    }

    /**
     * True when the token carries a purpose and the request is not that purpose's endpoint.
     * Unknown purposes are accepted nowhere.
     */
    private boolean isUsedOutsideItsEndpoint(String purpose, HttpServletRequest request) {
        if (!StringUtils.hasText(purpose)) {
            return false;
        }
        final boolean isPasswordSetupCall = "POST".equalsIgnoreCase(request.getMethod())
                && PasswordConfirmJwtFilter.TARGET_PATH.equals(request.getServletPath());
        return !(WmpJwtClaims.PURPOSE_PASSWORD_SETUP.equals(purpose) && isPasswordSetupCall);
    }

    /**
     * Checks if the Authorization header contains a Bearer token.
     */
    private boolean isTokenPresent(String authHeader) {
        if (!StringUtils.hasText(authHeader)) return false;
        String expectedPrefix = jwtProperties.getPrefix() == null ? "Bearer" : jwtProperties.getPrefix().trim();
        return authHeader.startsWith(expectedPrefix);
    }

    /**
     * Extracts the JWT from the Authorization header.
     */
    private String extractToken(String authHeader) {
        String expectedPrefix = jwtProperties.getPrefix() == null ? "Bearer" : jwtProperties.getPrefix().trim();
        return authHeader.substring(expectedPrefix.length()).trim();
    }
}
