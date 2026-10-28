package raff.stein.customer.service.mifid.workflow.step.state.impl;

import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.repository.mifid.MifidFillingRepository;
import raff.stein.customer.repository.mifid.MifidQuestionnaireRepository;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.platformcore.exception.types.conflict.InvalidStateTransitionException;

import java.util.Objects;
import java.util.UUID;

@Component
public class SubmitMifidStateHandler extends BaseMifidStateHandler {

    protected SubmitMifidStateHandler(
            MifidFillingRepository mifidFillingRepository,
            MifidQuestionnaireRepository mifidQuestionnaireRepository) {
        super(mifidFillingRepository, mifidQuestionnaireRepository);
    }

    @Override
    public void validateStatus(MifidFillingStatus currentStatus, MifidActionType actionType) {
        // No strict validation for other actions
        if (Objects.requireNonNull(actionType) == MifidActionType.SUBMIT
                && (currentStatus == null || !currentStatus.isSubmittable())) {
            throw InvalidStateTransitionException.of(
                    "submit MIFID filling", currentStatus, MifidFillingStatus.submittableStatuses());
        }
    }

    @Override
    public void calculateAndSetNewStatus(MifidFilling mifidFilling, UUID customerId) {
        final MifidFillingEntity mifidFillingEntity = findTargetFilling(mifidFilling, customerId)
                .orElseThrow(() -> new IllegalStateException("No existing Mifid filling found for customer with ID: " + customerId));
        mifidFillingEntity.setStatus(MifidFillingStatus.SUBMITTED);
        mifidFilling.setStatus(MifidFillingStatus.SUBMITTED);
    }
}
