package raff.stein.user.event.consumer;

import io.cloudevents.CloudEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openapitools.model.CreateUserEvent;
import org.openapitools.model.CustomerUserResolvedEvent;
import org.openapitools.model.User;
import raff.stein.user.event.producer.CustomerUserResolvedEventPublisher;
import raff.stein.user.event.producer.UserCreatedEventPublisher;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.repository.UserRepository;
import raff.stein.user.security.JwtTokenIssuer;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CreateUserEventConsumerTest {

    private static final String EMAIL = "client@bank.test";
    private static final UUID USER_ID = UUID.randomUUID();

    private UserRepository userRepository;
    private UserCreatedEventPublisher userCreatedEventPublisher;
    private CustomerUserResolvedEventPublisher resolvedEventPublisher;
    private JwtTokenIssuer jwtTokenIssuer;
    private CreateUserEventConsumer consumer;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userCreatedEventPublisher = mock(UserCreatedEventPublisher.class);
        resolvedEventPublisher = mock(CustomerUserResolvedEventPublisher.class);
        jwtTokenIssuer = mock(JwtTokenIssuer.class);
        when(jwtTokenIssuer.issueClientSetupToken(anyString(), anyString())).thenReturn("client-token");
        when(jwtTokenIssuer.issuePasswordSetupToken(anyString(), anyString())).thenReturn("signup-token");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(call -> UserEntity.builder()
                .id(USER_ID)
                .email(call.<UserEntity>getArgument(0).getEmail())
                .build());
        // the payload parsing is the base class's job, covered in platform-core: only the handling is tested here
        consumer = new CreateUserEventConsumer(userRepository, userCreatedEventPublisher, resolvedEventPublisher, jwtTokenIssuer) {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<T> getEventPayload(CloudEvent cloudEvent, Class<T> eventType) {
                return (Optional<T>) Optional.of(event);
            }
        };
    }

    private CreateUserEvent event;

    private void consume(CreateUserEvent createUserEvent) {
        event = createUserEvent;
        final CloudEvent cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getId()).thenReturn("event-1");
        consumer.consume(cloudEvent);
    }

    private static CreateUserEvent clientEvent() {
        return new CreateUserEvent()
                .user(new User().email(EMAIL).firstName("Ada"))
                .bankCode("BANK_A")
                .branchCode("BR_01")
                .requestedBy("advisor@bank.test");
    }

    @Test
    void shouldCreateTheLoginWithTheClientTokenAndResolveIt() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        consume(clientEvent());

        verify(userCreatedEventPublisher).publishUserCreatedEvent(any(), eq("client-token"));
        final ArgumentCaptor<CustomerUserResolvedEvent> resolved = ArgumentCaptor.forClass(CustomerUserResolvedEvent.class);
        verify(resolvedEventPublisher).publish(resolved.capture());
        assertEquals(USER_ID, resolved.getValue().getUserId());
        assertEquals("BANK_A", resolved.getValue().getBankCode());
        assertEquals("BR_01", resolved.getValue().getBranchCode());
        assertEquals("advisor@bank.test", resolved.getValue().getRequestedBy());
    }

    @Test
    void shouldResolveAnExistingLoginWithoutCreatingOrEmailing() {
        final UserEntity existing = UserEntity.builder().id(USER_ID).email("Client@Bank.test").build();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(existing));

        consume(clientEvent());

        verify(userRepository, never()).save(any());
        verifyNoInteractions(userCreatedEventPublisher, jwtTokenIssuer);
        verify(resolvedEventPublisher).publish(any());
    }

    @Test
    void shouldUseTheShortTokenAndResolveNothingWithoutABank() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        consume(new CreateUserEvent().user(new User().email(EMAIL)));

        verify(userCreatedEventPublisher).publishUserCreatedEvent(any(), eq("signup-token"));
        verifyNoInteractions(resolvedEventPublisher);
    }
}
