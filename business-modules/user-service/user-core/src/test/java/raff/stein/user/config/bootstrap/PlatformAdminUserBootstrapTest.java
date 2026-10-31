package raff.stein.user.config.bootstrap;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.user.model.user.User;
import raff.stein.user.repository.UserRepository;
import raff.stein.user.security.JwtTokenIssuer;
import raff.stein.user.service.UserService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformAdminUserBootstrapTest {

    @Test
    void shouldCreateOnlyMissingPlatformAdminsWithoutRoles() {
        final UserRepository userRepository = mock(UserRepository.class);
        final UserService userService = mock(UserService.class);
        final JwtTokenIssuer jwtTokenIssuer = mock(JwtTokenIssuer.class);
        when(userRepository.existsByEmail("pa-1@wmp.local")).thenReturn(true);
        final List<String> actors = new ArrayList<>();
        when(userService.createUserEntity(any())).thenAnswer(invocation -> {
            actors.add(SecurityContextHolder.getContext().getEmail());
            final User user = invocation.getArgument(0);
            user.setId("new-id");
            return user;
        });
        when(jwtTokenIssuer.issuePasswordSetupToken(anyString(), anyString())).thenReturn("setup-token");

        new PlatformAdminUserBootstrap(userRepository, userService, jwtTokenIssuer, List.of("pa-1@wmp.local", " pa-2@wmp.local ", ""))
                .run(null);

        final ArgumentCaptor<User> created = ArgumentCaptor.forClass(User.class);
        verify(userService, times(1)).createUserEntity(created.capture());
        assertEquals("pa-2@wmp.local", created.getValue().getEmail());
        assertTrue(created.getValue().getBranchRoles().isEmpty());
        verify(userService).publishUser(any(), eq("setup-token"));
        assertEquals(List.of("bootstrap"), actors);
        assertNull(SecurityContextHolder.getContext());
    }
}
