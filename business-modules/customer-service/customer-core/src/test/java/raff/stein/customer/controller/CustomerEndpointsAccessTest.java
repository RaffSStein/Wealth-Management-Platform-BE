package raff.stein.customer.controller;

import org.junit.jupiter.api.Test;
import org.openapitools.api.CustomerApi;
import org.openapitools.api.MifidApi;
import org.openapitools.api.OnboardingApi;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
        final RequestMapping mapping = apiMethod.getAnnotation(RequestMapping.class);
        assertNotNull(mapping, apiMethod.getName() + " has no @RequestMapping");
        return Arrays.asList(mapping.method()).contains(RequestMethod.GET)
                ? CustomerAccessLevel.READ
                : CustomerAccessLevel.WRITE;
    }
}
