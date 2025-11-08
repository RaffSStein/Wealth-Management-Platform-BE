package raff.stein.user.model.auth;

public enum PlatformRole {
    CUSTOMER,
    ADVISOR;

    public static PlatformRole fromString(String value) {
        if (value == null) return CUSTOMER;
        return "ADVISOR".equalsIgnoreCase(value) ? ADVISOR : CUSTOMER;
    }
}

