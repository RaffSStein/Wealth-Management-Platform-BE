package raff.stein.user.model.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType; // e.g., Bearer
    private long expiresIn;   // seconds
}

