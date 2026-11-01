package raff.stein.platformcore.model.tenant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "platform.tenant")
public class TenantProperties {

    /**
     * Lets a PLATFORM_ADMIN read every bank's tenant-scoped rows in this service. Off by default: turn it on only in a
     * service whose endpoints restrict what a platform admin may do.
     */
    private boolean rootReadEnabled = false;
}
