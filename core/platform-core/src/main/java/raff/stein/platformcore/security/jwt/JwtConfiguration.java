package raff.stein.platformcore.security.jwt;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import raff.stein.platformcore.security.context.SecurityContextFilter;

import java.security.PublicKey;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfiguration {

    private final ResourceLoader resourceLoader;

    public JwtConfiguration(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public JwtTokenParser jwtTokenParser(JwtProperties properties) {
        PublicKey publicKey = JwtPublicKeyProvider.loadPublicKey(resourceLoader.getResource(properties.getPublicKeyPath()));
        return new JwtTokenParser(publicKey);
    }

    /**
     * JwtDecoder used by Spring Security OAuth2 Resource Server.
     */
    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        PublicKey publicKey = JwtPublicKeyProvider.loadPublicKey(resourceLoader.getResource(properties.getPublicKeyPath()));
        return NimbusJwtDecoder.withPublicKey((java.security.interfaces.RSAPublicKey) publicKey).build();
    }

    /**
     * Exposes the custom security context filter as a bean so it can be ordered inside the Spring SecurityFilterChain.
     */
    @Bean
    public SecurityContextFilter securityContextFilter(
            JwtTokenParser parser,
            JwtProperties properties,
            TokenRevocationService tokenRevocationService) {
        return new SecurityContextFilter(parser, properties, tokenRevocationService);
    }
}
