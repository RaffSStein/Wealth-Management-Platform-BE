package raff.stein.customer.service.mifid.workflow.step.state.impl;

import org.springframework.stereotype.Component;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.repository.mifid.MifidFillingRepository;
import raff.stein.customer.repository.mifid.MifidQuestionnaireRepository;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;

import java.util.Objects;

@Component
public class UpdateMifidStateHandler extends BaseMifidStateHandler {

    public UpdateMifidStateHandler(
            MifidFillingRepository mifidFillingRepository,
            MifidQuestionnaireRepository mifidQuestionnaireRepository) {
        super(mifidFillingRepository, mifidQuestionnaireRepository);
    }

    @Override
    public void validateStatus(MifidFillingStatus currentStatus, MifidActionType actionType) {
        // only allow updates if the current status is updatable
        if (Objects.requireNonNull(actionType) == MifidActionType.UPDATE && !currentStatus.isUpdatable()) {
            throw new IllegalStateException("Cannot update MIFID unless it is" +
                    MifidFillingStatus.updatableStatuses() +
                    ". Current status: " +
                    currentStatus);
        }
    }
}
