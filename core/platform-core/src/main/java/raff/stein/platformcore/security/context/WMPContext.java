package raff.stein.platformcore.security.context;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.Set;

@ToString
@Builder
@Data
public class WMPContext {

    private final String userId;
    private final String email;
    private final Set<String> roles;
    private final String bankCode;          //TODO: rename to branchCode
    private final String correlationId;
    // Raw JWT token for downstream services that need to re-validate specific flows (e.g., password setup)
    private final String rawToken;
    // JWT ID (jti) for revocation/one-time flows
    private final String jti;
    // Token expiration epoch seconds (for revocation TTL and audit)
    private final Long tokenExpEpochSeconds;

}
