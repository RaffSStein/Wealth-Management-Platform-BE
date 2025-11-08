package raff.stein.user.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import raff.stein.user.model.auth.PlatformRole;
import raff.stein.user.model.entity.BranchUserEntity;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.repository.UserRepository;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity entity = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        if (!entity.isEnabled()) {
            throw new UsernameNotFoundException("User disabled: " + username);
        }
        Set<PlatformRole> platformRoles = resolvePlatformRoles(entity);
        Collection<? extends GrantedAuthority> authorities = platformRoles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                .toList();
        return new User(entity.getEmail(), entity.getPasswordHash(), authorities);
    }

    /**
     * Resolves ALL platform roles for the user across branches.
     * If none are present, CUSTOMER fallback is applied (with a warning).
     */
    private Set<PlatformRole> resolvePlatformRoles(UserEntity entity) {
        Set<PlatformRole> roles = entity.getBankBranchUsers().stream()
                .map(BranchUserEntity::getRole)
                .map(PlatformRole::fromString)
                .collect(Collectors.toSet());
        if (roles.isEmpty()) {
            log.warn("Role fallback applied in UserDetailsService: assigning CUSTOMER for user [{}] (no branch associations)", entity.getEmail());
            roles.add(PlatformRole.CUSTOMER);
        }
        return roles;
    }
}
