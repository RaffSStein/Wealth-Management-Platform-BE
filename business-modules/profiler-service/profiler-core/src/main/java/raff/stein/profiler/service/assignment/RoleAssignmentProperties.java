package raff.stein.profiler.service.assignment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "wmp.role-assignment")
public class RoleAssignmentProperties {

    /**
     * Staff assignments, approvals and admin revocations need a login at most this old.
     */
    private Duration recentLogin = Duration.ofMinutes(10);

    /**
     * A pending request older than this can no longer be approved: it becomes EXPIRED when someone tries.
     */
    private Duration requestTtl = Duration.ofDays(7);

    /**
     * Emails that get a PLATFORM_ADMIN assignment when their user-created event arrives (first platform run).
     * At least two are needed: with four-eyes approval one platform admin alone can't create a bank admin.
     */
    private List<String> bootstrapPlatformAdmins = new ArrayList<>();
}
