package raff.stein.platformcore.security.role;

import java.util.Arrays;
import java.util.Optional;

/**
 * Platform roles, from least to most privileged. A role is held through a role assignment (user + bank + branch),
 * owned by profiler-service; the token carries the role of the active assignment. PLATFORM_ADMIN is group-level and
 * created only by the bootstrap. See docs/domain/process/security/roles-and-permissions.md.
 */
public enum WmpRole {
    CUSTOMER,
    ADVISOR,
    BANK_ADMIN,
    PLATFORM_ADMIN;

    /**
     * Exact, case-insensitive parsing. An unknown role grants nothing: it never falls back to another role.
     */
    public static Optional<WmpRole> parse(String value) {
        if (value == null) return Optional.empty();
        return Arrays.stream(values()).filter(role -> role.name().equalsIgnoreCase(value.trim())).findFirst();
    }

    public boolean isStaff() {
        return this != CUSTOMER;
    }
}
