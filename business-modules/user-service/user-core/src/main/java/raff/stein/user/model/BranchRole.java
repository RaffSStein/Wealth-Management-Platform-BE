package raff.stein.user.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchRole {

    // role assignment id in profiler-service (the id to revoke it with)
    private Long id;

    private String bankCode;
    private String branchCode;
    private String role;
    // the role assignment the current token was issued for; computed on read, never stored
    private boolean active;
}
