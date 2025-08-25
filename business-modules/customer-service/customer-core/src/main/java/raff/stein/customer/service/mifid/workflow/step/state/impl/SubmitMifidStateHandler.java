package raff.stein.customer.service.mifid.workflow.step.state.impl;

import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.repository.mifid.MifidFillingRepository;
import raff.stein.customer.repository.mifid.MifidQuestionnaireRepository;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class SubmitMifidStateHandler extends BaseMifidStateHandler {

    private final MifidFillingRepository mifidFillingRepository;

    protected SubmitMifidStateHandler(
            MifidFillingRepository mifidFillingRepository,
            MifidQuestionnaireRepository mifidQuestionnaireRepository) {
        super(mifidFillingRepository, mifidQuestionnaireRepository);
        this.mifidFillingRepository = mifidFillingRepository;
    }

    @Override
    public void validateStatus(MifidFillingStatus currentStatus, MifidActionType actionType) {
        // No strict validation for other actions
        if (Objects.requireNonNull(actionType) == MifidActionType.SUBMIT && !currentStatus.isSubmittable()) {
            throw new IllegalStateException("Cannot submit MIFID unless it is" +
                    MifidFillingStatus.submittableStatuses() +
                    ". Current status: " +
                    currentStatus);
        }
    }

    @Override
    public void calculateAndSetNewStatus(MifidFilling mifidFilling, UUID customerId) {
        Optional<MifidFillingEntity> mifidFillingEntityOptional = mifidFillingRepository
                .findTopByCustomerIdAndStatusNotOrderByFillingDateDesc(customerId, MifidFillingStatus.DEPRECATED);

        mifidFillingEntityOptional.ifPresentOrElse(
                mifidFillingEntity -> {
                    mifidFillingEntity.setStatus(MifidFillingStatus.SUBMITTED);
                    mifidFilling.setStatus(MifidFillingStatus.SUBMITTED);
                },
                () -> {
                    // if no existing filling is found, we cannot calculate status
                    throw new IllegalStateException("No existing Mifid filling found for customer with ID: " + customerId);
                });
    }
}
