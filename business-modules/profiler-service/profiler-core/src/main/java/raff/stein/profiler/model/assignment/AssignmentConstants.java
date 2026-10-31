package raff.stein.profiler.model.assignment;

public final class AssignmentConstants {

    /**
     * Bank and branch code of PLATFORM_ADMIN assignments: group-level, not a real bank, so no bank data is visible
     * through them (no tenant-scoped row has this tenant).
     */
    public static final String PLATFORM_TENANT = "PLATFORM";

    public static final String BOOTSTRAP_ACTOR = "bootstrap";

    private AssignmentConstants() {
    }
}
