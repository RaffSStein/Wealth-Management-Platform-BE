package raff.stein.platformcore.security.jwt;

/**
 * Custom JWT claim names shared by the token issuer (user-service) and the parser (every service).
 */
public final class WmpJwtClaims {

    private WmpJwtClaims() {
        // Prevent instantiation
    }

    public static final String USER_ID = "userId";
    public static final String EMAIL = "email";
    public static final String ROLES = "roles";
    // bank code of the active membership
    public static final String TENANT_ID = "tenantId";
    // branch of the active membership
    public static final String BRANCH_CODE = "branchCode";
    // set only on single-purpose tokens: they are accepted on their own endpoint and nowhere else
    public static final String PURPOSE = "purpose";
    public static final String PURPOSE_PASSWORD_SETUP = "PASSWORD_SETUP";
}
