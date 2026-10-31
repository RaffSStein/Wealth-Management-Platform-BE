package raff.stein.bank.service.visibility;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.role.WmpRole;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

/**
 * Which banks and branches a caller may list or read: a PLATFORM_ADMIN all of them, any other role only the bank of
 * its token (the banks it may assign roles in), a token without a bank none. Out of scope answers like "not found".
 * See docs/domain/process/security/roles-and-permissions.md.
 */
@Component
public class BankVisibilityPolicy {

    public BankScope scopeOf(WMPContext caller) {
        if (caller == null) {
            return BankScope.none();
        }
        final Optional<WmpRole> role = Optional.ofNullable(caller.getRoles()).orElse(Set.of()).stream()
                .map(WmpRole::parse)
                .flatMap(Optional::stream)
                .max(Comparator.naturalOrder());
        if (role.isPresent() && role.get() == WmpRole.PLATFORM_ADMIN) {
            return BankScope.allBanks();
        }
        return StringUtils.hasText(caller.getTenantId()) ? BankScope.only(caller.getTenantId()) : BankScope.none();
    }
}
