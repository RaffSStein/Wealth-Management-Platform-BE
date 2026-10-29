package raff.stein.customer.model.entity.mifid.enumeration;

import java.util.Set;

/**
 * Lifecycle of a MiFID filling: DRAFT -> COMPLETED -> SUBMITTED, then DEPRECATED once superseded.
 * DRAFT and COMPLETED are derived from the answers on every save/update; SUBMITTED is set only by an explicit submit.
 */
public enum MifidFillingStatus {

    // not all questions answered yet; the only status that accepts updates
    DRAFT,
    // all questions answered but not submitted yet: no risk profile calculated, can still be submitted
    COMPLETED,
    // superseded by a newer filling (e.g. MiFID renewal); kept for history and ignored by lookups
    DEPRECATED,
    // explicitly submitted by the customer: risk profile calculated and persisted, MiFID onboarding step done
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
