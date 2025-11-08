package raff.stein.user.model.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BranchRoleRequest {
    @NotBlank
    private String bankCode;
    // In current domain naming, this maps to event's branchCode
    @NotBlank
    private String bankId;
    @NotBlank
    private String role;
}

