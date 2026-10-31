package raff.stein.profiler.model.assignment;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AssignRoleCommand {

    private UUID userId;
    private String branchCode;
    private String role;
    // only honoured for PLATFORM_ADMIN callers; everyone else acts in the bank of their token
    private String bankCode;
}
