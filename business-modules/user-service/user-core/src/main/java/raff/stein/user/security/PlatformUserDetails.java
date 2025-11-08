package raff.stein.user.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Custom UserDetails carrying the userId so downstream logic (e.g. JWT issuance) does not need an extra DB lookup.
 */
@Getter
public class PlatformUserDetails implements UserDetails {

    private final String userId;
    private final String username; // email
    private final String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public PlatformUserDetails(String userId,
                               String username,
                               String passwordHash,
                               Collection<? extends GrantedAuthority> authorities,
                               boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.authorities = authorities;
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

