package raff.stein.user.model.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    // optional active role assignment; the first one is used when absent
    private String bankCode;
    private String branchCode;
}
