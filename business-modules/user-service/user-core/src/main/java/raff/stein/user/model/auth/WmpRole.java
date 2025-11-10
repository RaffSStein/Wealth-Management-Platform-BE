package raff.stein.user.model.auth;

public enum WmpRole {
    CUSTOMER,
    ADVISOR;

    public static WmpRole fromString(String value) {
        if (value == null) return CUSTOMER;
        return "ADVISOR".equalsIgnoreCase(value) ? ADVISOR : CUSTOMER;
    }
}

