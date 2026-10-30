package raff.stein.user.model.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BranchRoleRequest {
    @NotBlank
    private String bankCode;
    @NotBlank
    private String branchCode;
    @NotBlank
    private String role;
}

