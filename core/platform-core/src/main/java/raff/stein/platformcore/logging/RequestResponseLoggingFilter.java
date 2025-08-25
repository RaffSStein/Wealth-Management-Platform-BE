package raff.stein.platformcore.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filter that logs all HTTP requests and responses, including payloads.
 * This filter is intended to be shared across all microservices for centralized logging.
 */
@Component
@Slf4j
public class RequestResponseLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        String path = httpRequest.getRequestURI();
        // Exclude actuator/health and actuator/prometheus endpoints from logging
        if (path.startsWith("/actuator/health") || path.startsWith("/actuator/prometheus")) {
            chain.doFilter(request, response);
            return;
        }
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper((HttpServletResponse) response);
        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            logRequest(wrappedRequest);
            logResponse(wrappedResponse);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        StringBuilder msg = new StringBuilder();
        msg.append("[REQUEST] ")
           .append(request.getMethod())
           .append(" ")
           .append(request.getRequestURI());
        if (request.getQueryString() != null) {
            msg.append('?').append(request.getQueryString());
        }
        String payload = getPayload(request.getContentAsByteArray());
        if (!payload.isEmpty()) {
            msg.append(" | Payload: ").append(payload.replaceAll("\\s+", ""));
        }
        String msgString = msg.toString();
        log.info(msgString);
    }

    private void logResponse(ContentCachingResponseWrapper response) {
        StringBuilder msg = new StringBuilder();
        msg.append("[RESPONSE] Status: ").append(response.getStatus());
        String payload = getPayload(response.getContentAsByteArray());
        if (!payload.isEmpty()) {
            msg.append(" | Payload: ").append(payload);
        }
        String msgString = msg.toString();
        log.info(msgString);
    }

    private String getPayload(byte[] buf) {
        if (buf == null || buf.length == 0) return "";
        return new String(buf, StandardCharsets.UTF_8);
    }
}
