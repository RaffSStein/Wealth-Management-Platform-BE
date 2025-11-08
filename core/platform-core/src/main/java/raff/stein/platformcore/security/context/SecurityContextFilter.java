package raff.stein.platformcore.security.context;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import raff.stein.platformcore.security.jwt.JwtProperties;
import raff.stein.platformcore.security.jwt.JwtTokenParser;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter that extracts the JWT from the request, parses it,
 * and stores the authenticated user in the custom WMP context.
 * It also sets the user information in the MDC for logging purposes.
 *
 * This filter is non-blocking: if no token is present or token is invalid,
 * it does not fail the request. Authorization is handled by Spring Security downstream.
 */
@Slf4j
public class SecurityContextFilter implements Filter {

    private final JwtTokenParser tokenParser;
    private final JwtProperties jwtProperties;

    public SecurityContextFilter(JwtTokenParser tokenParser, JwtProperties jwtProperties) {
        this.tokenParser = tokenParser;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String authHeader = httpRequest.getHeader(jwtProperties.getHeader());
            String correlationId = httpRequest.getHeader("X-Correlation-ID");

            if (isTokenPresent(authHeader)) {
                String token = extractToken(authHeader);
                Optional<WMPContext> wmpContextOptional = tokenParser.parseTokenAndBuildContext(token, correlationId);
                wmpContextOptional.ifPresent(context -> {
                    // Set the context in custom SecurityContextHolder (domain-specific, not Spring's)
                    SecurityContextHolder.setContext(context);
                    // Set MDC for logging
                    MDC.put("userId", context.getUserId());
                    MDC.put("email", context.getEmail());
                    MDC.put("bankCode", context.getBankCode());
                    MDC.put("correlationId", context.getCorrelationId());
                });
            }
            // If no token present or invalid, we let Spring Security decide authorization later.

            chain.doFilter(request, response);

        } finally {
            SecurityContextHolder.clear();
            MDC.clear();
        }
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
