package raff.stein.customer.service.mifid.workflow.step.state;

import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;

import java.util.UUID;

public interface MifidStateHandler {

    void validate(
            MifidFilling mifidFilling,
            UUID customerId,
            MifidActionType actionType);

    void calculateAndSetNewStatus(
            MifidFilling mifidFilling,
            UUID customerId);

}
