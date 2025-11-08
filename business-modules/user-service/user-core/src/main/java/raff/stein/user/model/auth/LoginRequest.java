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

    /**
     * Selected bank code for the session scope. Used to scope permissions.
     */
    @NotBlank
    private String bankCode;
}
