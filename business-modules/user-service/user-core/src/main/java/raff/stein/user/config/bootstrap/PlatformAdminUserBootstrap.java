package raff.stein.user.config.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.user.model.user.User;
import raff.stein.user.repository.UserRepository;
import raff.stein.user.security.JwtTokenIssuer;
import raff.stein.user.service.UserService;

import java.util.List;

/**
 * First platform run: creates the configured platform-admin users if they don't exist yet and sends them the usual
 * onboarding email (password-setup link). profiler-service gives them PLATFORM_ADMIN when it sees their
 * user-created event. Idempotent: existing users are left alone. See docs/domain/process/platform/first-platform-run.md.
 */
@Slf4j
@Component
public class PlatformAdminUserBootstrap implements ApplicationRunner {

    static final String BOOTSTRAP_ACTOR = "bootstrap";

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenIssuer jwtTokenIssuer;
    private final List<String> platformAdminEmails;

    public PlatformAdminUserBootstrap(
            UserRepository userRepository,
            UserService userService,
            JwtTokenIssuer jwtTokenIssuer,
            @Value("${wmp.bootstrap.platform-admins:}") List<String> platformAdminEmails) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtTokenIssuer = jwtTokenIssuer;
        this.platformAdminEmails = platformAdminEmails;
    }

    @Override
    public void run(ApplicationArguments args) {
        // no user is behind a startup action: act as a named system actor, so audit columns and the events'
        // actor extension say "bootstrap" (consumers reject writes without an actor)
        SecurityContextHolder.setContext(WMPContext.builder().userId(BOOTSTRAP_ACTOR).email(BOOTSTRAP_ACTOR).build());
        try {
            platformAdminEmails.stream()
                    .map(String::trim)
                    .filter(email -> !email.isEmpty())
                    .forEach(this::createIfMissing);
        } finally {
            SecurityContextHolder.clear();
        }
    }

    private void createIfMissing(String email) {
        if (userRepository.existsByEmail(email)) {
            log.debug("Bootstrap platform admin [{}] already exists", email);
            return;
        }
        final User user = userService.createUserEntity(User.builder()
                .email(email)
                .firstName("Platform")
                .lastName("Admin")
                .phoneNumber("-")
                .branchRoles(List.of())
                .build());
        userService.publishUser(user, jwtTokenIssuer.issuePasswordSetupToken(user.getId(), user.getEmail()));
        log.info("Bootstrap: created platform admin user [{}]; the password-setup link was sent by email", email);
    }
}
