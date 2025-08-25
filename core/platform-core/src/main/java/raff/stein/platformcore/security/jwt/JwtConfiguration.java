package raff.stein.platformcore.security.jwt;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
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

    @Bean
    public FilterRegistrationBean<SecurityContextFilter> securityContextFilter(JwtTokenParser parser, JwtProperties properties) {
        FilterRegistrationBean<SecurityContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityContextFilter(parser, properties));
        registration.setOrder(1);
        registration.addUrlPatterns("/*"); // Apply to all endpoints
        return registration;
    }
}
