package raff.stein.customer.controller;

import org.junit.jupiter.api.Test;
import io.openapiprocessor.openapi.api.CustomerApi;
import io.openapiprocessor.openapi.api.MifidApi;
import io.openapiprocessor.openapi.api.OnboardingApi;
import org.springframework.web.bind.annotation.GetMapping;
import raff.stein.customer.controller.access.CustomerAccess;
import raff.stein.customer.controller.access.CustomerAccessLevel;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Walks the generated API interfaces: every endpoint on a customer must declare the access level matching its HTTP
 * method, so a write can't be marked as a read by mistake.
 */
class CustomerEndpointsAccessTest {

    @Test
    void shouldDeclareTheRightAccessLevelOnEveryCustomerEndpoint() throws Exception {
        int checked = 0;
        checked += checkAll(CustomerApi.class, CustomerController.class);
        checked += checkAll(MifidApi.class, MifidController.class);
        checked += checkAll(OnboardingApi.class, OnboardingController.class);

        // 4 + disambiguation in CustomerApi, 5 in MifidApi, 3 in OnboardingApi: fails if the walk stops finding them
        assertEquals(13, checked);
    }

    @Test
    void shouldScopeTheListEndpoints() throws Exception {
        int checked = 0;
        for (final Method method : CustomerController.class.getDeclaredMethods()) {
            if (method.isSynthetic()) {
                continue;
            }
            if (method.getName().equals("getCustomers")) {
                assertEquals(CustomerAccessLevel.LIST, method.getAnnotation(CustomerAccess.class).value());
                checked++;
            }
            if (method.getName().equals("getAdvisorDashboardStatistics")) {
                assertEquals(CustomerAccessLevel.ADVISOR_DASHBOARD, method.getAnnotation(CustomerAccess.class).value());
                checked++;
            }
        }
        assertEquals(2, checked);
    }

    private static int checkAll(Class<?> api, Class<?> controller) throws Exception {
        int checked = 0;
        for (final Method apiMethod : api.getMethods()) {
            // the only UUID path parameters are customerId and, on the disambiguation operation, userId
            if (!Arrays.asList(apiMethod.getParameterTypes()).contains(UUID.class)) {
                continue;
            }
            final CustomerAccess access = controller
                    .getMethod(apiMethod.getName(), apiMethod.getParameterTypes())
                    .getAnnotation(CustomerAccess.class);
            assertNotNull(access, apiMethod.getName() + " has no @CustomerAccess");
            assertEquals(expectedLevel(apiMethod), access.value(), apiMethod.getName());
            checked++;
        }
        return checked;
    }

    private static CustomerAccessLevel expectedLevel(Method apiMethod) {
        if (apiMethod.getName().equals("getCustomersByUserId")) {
            return CustomerAccessLevel.LIST_BY_USER;
        }
        // the processor generates @GetMapping, @PostMapping...
        return apiMethod.isAnnotationPresent(GetMapping.class)
                ? CustomerAccessLevel.READ
                : CustomerAccessLevel.WRITE;
    }
}
