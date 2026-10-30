package raff.stein.platformcore.messaging;

/**
 * CloudEvents extension attribute names used to forward the WMP context from producers to consumers.
 * Extension names must be lower-case alphanumeric (CloudEvents spec).
 */
public final class CloudEventExtensions {

    private CloudEventExtensions() {
        // Prevent instantiation
    }

    public static final String USER_ID = "userid";
    public static final String EMAIL = "email";
    public static final String TENANT_ID = "tenantid";
    public static final String BRANCH_CODE = "branchcode";
    public static final String CORRELATION_ID = "correlationid";
}
