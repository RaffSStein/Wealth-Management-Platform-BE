package raff.stein.customer.model.entity.customer.enumeration;

import lombok.Getter;

@Getter
public enum OnboardingStep {

    INIT(1),                   // step 1
    DOCUMENTS(2),              // step 1.1
    FINANCIALS(3),             // step 2
    GOALS(4),                  // step 3
    MIFID(5),                  // step 4
    AML(6),                    // step 5
    FINALIZE(7);               // step 6

    private final int order;

    OnboardingStep(int order) {
        this.order = order;
    }

}
