package raff.stein.profiler.model.assignment;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class RoleAssignment {

    private Long id;
    private UUID userId;
    private String userEmail;
    private String bankCode;
    private String branchCode;
    private String role;
    private String grantedBy;
    private String approvedBy;
    private Instant grantedAt;
}
