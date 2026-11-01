package raff.stein.customer.event.consumer;

import io.cloudevents.CloudEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.model.RoleAssignmentGrantedEvent;
import org.openapitools.model.RoleAssignmentRevokedEvent;
import raff.stein.customer.service.link.CustomerUserLinkService;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;

import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

class RoleAssignmentEventConsumersTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "client@bank.test";

    private CustomerUserLinkService linkService;

    @BeforeEach
    void setUp() {
        linkService = mock(CustomerUserLinkService.class);
        SecurityContextHolder.setContext(WMPContext.builder().tenantId("BANK_A").build());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    // the payload parsing is the base class's job, covered in platform-core: only the handling is tested here
    private void consume(RoleAssignmentGrantedEvent event) {
        new RoleAssignmentGrantedEventConsumer(linkService) {
            @Override
            @SuppressWarnings("unchecked")
            public <T> void withEventPayload(CloudEvent cloudEvent, Class<T> clazz, Consumer<T> consumer) {
                consumer.accept((T) event);
            }
        }.consume(mock(CloudEvent.class));
    }

    private void consume(RoleAssignmentRevokedEvent event) {
        new RoleAssignmentRevokedEventConsumer(linkService) {
            @Override
            @SuppressWarnings("unchecked")
            public <T> void withEventPayload(CloudEvent cloudEvent, Class<T> clazz, Consumer<T> consumer) {
                consumer.accept((T) event);
            }
        }.consume(mock(CloudEvent.class));
    }

    private static RoleAssignmentGrantedEvent granted(String role, String bankCode) {
        return new RoleAssignmentGrantedEvent()
                .userId(USER_ID).email(EMAIL).bankCode(bankCode).branchCode("BR_01").role(role);
    }

    private static RoleAssignmentRevokedEvent revoked(String role, String bankCode) {
        return new RoleAssignmentRevokedEvent()
                .userId(USER_ID).email(EMAIL).bankCode(bankCode).branchCode("BR_01").role(role);
    }

    @Test
    void shouldLinkOnACustomerRole() {
        consume(granted("CUSTOMER", "BANK_A"));

        verify(linkService).linkOnRoleGranted(USER_ID, EMAIL, "BR_01");
    }

    @Test
    void shouldUnlinkOnARevokedCustomerRole() {
        consume(revoked("CUSTOMER", "BANK_A"));

        verify(linkService).unlinkOnRoleRevoked(USER_ID, "BR_01");
    }

    @Test
    void shouldIgnoreOtherRoles() {
        consume(granted("ADVISOR", "BANK_A"));
        consume(revoked("BANK_ADMIN", "BANK_A"));

        verifyNoInteractions(linkService);
    }

    @Test
    void shouldIgnoreAnEventOfAnotherTenant() {
        consume(granted("CUSTOMER", "BANK_B"));
        consume(revoked("CUSTOMER", "BANK_B"));

        verifyNoInteractions(linkService);
    }

    @Test
    void shouldIgnoreAnEventWithMissingFields() {
        consume(new RoleAssignmentGrantedEvent().role("CUSTOMER").bankCode("BANK_A"));
        consume(new RoleAssignmentRevokedEvent().role("CUSTOMER").bankCode("BANK_A"));

        verifyNoInteractions(linkService);
    }
}
