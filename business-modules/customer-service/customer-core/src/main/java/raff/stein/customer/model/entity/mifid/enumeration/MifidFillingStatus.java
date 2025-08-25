package raff.stein.customer.model.entity.mifid.enumeration;

import java.util.Set;

public enum MifidFillingStatus {

    DRAFT,
    COMPLETED,
    DEPRECATED,
    SUBMITTED;

    public static Set<MifidFillingStatus> updatableStatuses() {
        return Set.of(DRAFT);
    }

    public static Set<MifidFillingStatus> finalStatuses() {
        return Set.of(SUBMITTED, DEPRECATED);
    }

    public static Set<MifidFillingStatus> submittableStatuses() {
        return Set.of(COMPLETED);
    }

    public boolean isSubmittable() {
        return submittableStatuses().contains(this);
    }

    public boolean isFinal() {
        return finalStatuses().contains(this);
    }

    public boolean isUpdatable() {
        return updatableStatuses().contains(this);
    }


}
