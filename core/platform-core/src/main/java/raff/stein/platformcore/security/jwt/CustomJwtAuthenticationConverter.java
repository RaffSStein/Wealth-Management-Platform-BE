package raff.stein.platformcore.security.jwt;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;

/**
 * Custom converter used during authentication to extract authorities from JWT.
 * It delegates to JwtGrantedAuthoritiesConverter but can be extended to map domain-specific claims
 * (e.g., permissions, scopes) to GrantedAuthority instances.
 */
public class CustomJwtAuthenticationConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter delegate;

    public CustomJwtAuthenticationConverter(String authoritiesClaimName, String authorityPrefix) {
        this.delegate = new JwtGrantedAuthoritiesConverter();
        this.delegate.setAuthoritiesClaimName(authoritiesClaimName);
        this.delegate.setAuthorityPrefix(authorityPrefix);
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt source) {
        return delegate.convert(source);
    }
}

