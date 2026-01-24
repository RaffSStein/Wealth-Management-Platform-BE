package raff.stein.customer.model.entity.customer.enumeration;

import lombok.Getter;

@Getter
public enum OnboardingStep {

    INIT(1),
    PERSONAL_DETAILS(2),
    DOCUMENTS(3),
    FINANCIALS(4),
    GOALS(5),
    MIFID(6),
    AML(7),
    FINALIZE(8);

    private final int order;

    OnboardingStep(int order) {
        this.order = order;
    }

}
