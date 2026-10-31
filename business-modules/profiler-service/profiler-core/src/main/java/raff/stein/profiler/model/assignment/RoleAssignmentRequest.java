package raff.stein.profiler.model.assignment;

import lombok.Builder;
import lombok.Data;
import raff.stein.profiler.model.entity.enumeration.RoleAssignmentRequestStatus;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class RoleAssignmentRequest {

    private Long id;
    private UUID userId;
    private String userEmail;
    private String bankCode;
    private String branchCode;
    private String role;
    private RoleAssignmentRequestStatus status;
    private String requestedBy;
    private Instant requestedAt;
    private String decidedBy;
    private Instant decidedAt;
    private String reason;
}
