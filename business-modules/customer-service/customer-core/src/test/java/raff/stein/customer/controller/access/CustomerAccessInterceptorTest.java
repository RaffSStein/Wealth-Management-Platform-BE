package raff.stein.customer.controller.access;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import raff.stein.customer.service.access.CustomerAccessPolicy;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CustomerAccessInterceptorTest {

    private static final UUID ID = UUID.randomUUID();

    @SuppressWarnings("unused")
    private static final class Endpoints {
        @CustomerAccess(CustomerAccessLevel.READ)
        public void read() {
            // endpoint stub: the interceptor only reads its annotation
        }

        @CustomerAccess(CustomerAccessLevel.WRITE)
        public void write() {
            // endpoint stub: the interceptor only reads its annotation
        }

        @CustomerAccess(CustomerAccessLevel.LIST_BY_USER)
        public void list() {
            // endpoint stub: the interceptor only reads its annotation
        }

        @CustomerAccess(CustomerAccessLevel.LIST)
        public void book() {
            // endpoint stub: the interceptor only reads its annotation
        }

        @CustomerAccess(CustomerAccessLevel.ADVISOR_DASHBOARD)
        public void dashboard() {
            // endpoint stub: the interceptor only reads its annotation
        }

        public void unannotated() {
            // endpoint stub: the interceptor only reads its annotation
        }
    }

    private CustomerAccessPolicy policy;
    private CustomerAccessInterceptor interceptor;

    @BeforeEach
    void setUp() {
        policy = mock(CustomerAccessPolicy.class);
        interceptor = new CustomerAccessInterceptor(policy);
    }

    private boolean call(String endpoint, Map<String, String> pathVariables) throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(pathVariables);
        final HandlerMethod handler = new HandlerMethod(new Endpoints(), Endpoints.class.getMethod(endpoint));
        return interceptor.preHandle(request, mock(HttpServletResponse.class), handler);
    }

    @Test
    void shouldCheckTheLevelDeclaredOnTheEndpoint() throws Exception {
        call("read", Map.of("customerId", ID.toString()));
        verify(policy).assertCanRead(ID);

        call("write", Map.of("customerId", ID.toString()));
        verify(policy).assertCanWrite(ID);

        call("list", Map.of("userId", ID.toString()));
        verify(policy).assertCanListByUser(ID);

        call("book", Map.of());
        verify(policy).assertCanList();

        call("dashboard", Map.of());
        verify(policy).assertCanReadAdvisorDashboard();
    }

    @Test
    void shouldRefuseCustomerEndpointWithoutAnnotation() {
        final Map<String, String> pathVariables = Map.of("customerId", ID.toString());

        assertThrows(AccessDeniedException.class, () -> call("unannotated", pathVariables));
    }

    @Test
    void shouldLeaveEndpointsNotOnACustomerAlone() throws Exception {
        assertTrue(call("unannotated", Map.of()));
        verifyNoInteractions(policy);
    }

    @Test
    void shouldLeaveMalformedIdToRequestBinding() throws Exception {
        assertTrue(call("read", Map.of("customerId", "not-a-uuid")));
        verifyNoInteractions(policy);
    }

    @Test
    void shouldIgnoreHandlersThatAreNotControllerMethods() {
        assertTrue(interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), new Object()));
        verifyNoInteractions(policy);
    }
}
