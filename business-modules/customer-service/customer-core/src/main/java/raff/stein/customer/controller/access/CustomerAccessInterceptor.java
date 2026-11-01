package raff.stein.customer.controller.access;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import raff.stein.customer.service.access.CustomerAccessPolicy;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Enforces {@link CustomerAccess}. It runs before the request body is read, so a caller out of scope gets the same
 * answer whatever it sends. A route on a customer without the annotation is refused, so forgetting it can't open data.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerAccessInterceptor implements HandlerInterceptor {

    private static final String CUSTOMER_ID = "customerId";
    private static final String USER_ID = "userId";

    private final CustomerAccessPolicy customerAccessPolicy;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        final Map<?, ?> pathVariables = pathVariables(request);
        final CustomerAccess access = handlerMethod.getMethodAnnotation(CustomerAccess.class);
        if (access == null) {
            if (pathVariables.containsKey(CUSTOMER_ID)) {
                log.error("Endpoint on a customer without @CustomerAccess: [{}]", handlerMethod.getShortLogMessage());
                throw new AccessDeniedException(ErrorCode.ACCESS_DENIED, "Endpoint without an access rule");
            }
            return true;
        }
        if (access.value() == CustomerAccessLevel.LIST) {
            customerAccessPolicy.assertCanList();
            return true;
        }
        if (access.value() == CustomerAccessLevel.ADVISOR_DASHBOARD) {
            customerAccessPolicy.assertCanReadAdvisorDashboard();
            return true;
        }
        final String variable = access.value() == CustomerAccessLevel.LIST_BY_USER ? USER_ID : CUSTOMER_ID;
        // a malformed id reaches no data: binding the controller argument fails right after this
        parseUuid(pathVariables.get(variable)).ifPresent(id -> {
            switch (access.value()) {
                case READ -> customerAccessPolicy.assertCanRead(id);
                case WRITE -> customerAccessPolicy.assertCanWrite(id);
                case LIST_BY_USER -> customerAccessPolicy.assertCanListByUser(id);
                case LIST, ADVISOR_DASHBOARD -> throw new IllegalStateException("Handled above");
            }
        });
        return true;
    }

    private static Map<?, ?> pathVariables(HttpServletRequest request) {
        return request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?, ?> variables
                ? variables
                : Map.of();
    }

    private static Optional<UUID> parseUuid(Object value) {
        try {
            return Optional.ofNullable(value).map(Object::toString).map(UUID::fromString);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
