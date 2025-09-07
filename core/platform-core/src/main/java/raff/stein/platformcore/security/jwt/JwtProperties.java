package raff.stein.platformcore.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * Location of the public key file (e.g., classpath:keys/public_key.pem)
     */
    private String publicKeyPath;
    private String header = "Authorization";
    private String prefix = "Bearer ";

}
