package raff.stein.customer.service.mifid.workflow.step.state.impl;

import org.springframework.stereotype.Component;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.repository.mifid.MifidFillingRepository;
import raff.stein.customer.repository.mifid.MifidQuestionnaireRepository;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.platformcore.exception.types.conflict.InvalidStateTransitionException;

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
        // a null status (no filling yet) is allowed: the update command inserts a new filling
        if (Objects.requireNonNull(actionType) == MifidActionType.UPDATE
                && currentStatus != null && !currentStatus.isUpdatable()) {
            throw InvalidStateTransitionException.of(
                    "update MIFID filling", currentStatus, MifidFillingStatus.updatableStatuses());
        }
    }
}
