package raff.stein.customer.service.mifid.workflow.step.state.impl;

import lombok.extern.slf4j.Slf4j;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.repository.mifid.MifidFillingRepository;
import raff.stein.customer.repository.mifid.MifidQuestionnaireRepository;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.customer.service.mifid.workflow.step.state.MifidStateHandler;

import java.util.Optional;
import java.util.UUID;

@Slf4j
public abstract class BaseMifidStateHandler implements MifidStateHandler {

    protected MifidFillingRepository mifidFillingRepository;
    protected MifidQuestionnaireRepository mifidQuestionnaireRepository;

    protected BaseMifidStateHandler(
            MifidFillingRepository mifidFillingRepository,
            MifidQuestionnaireRepository mifidQuestionnaireRepository) {
        this.mifidFillingRepository = mifidFillingRepository;
        this.mifidQuestionnaireRepository = mifidQuestionnaireRepository;
    }

    public abstract void validateStatus(MifidFillingStatus currentStatus, MifidActionType actionType);

    @Override
    public void validate(MifidFilling mifidFilling, UUID customerId, MifidActionType actionType) {
        MifidFillingStatus currentStatus = mifidFillingRepository
                .findTopByCustomerIdAndStatusNotOrderByFillingDateDesc(customerId, MifidFillingStatus.DEPRECATED)
                .map(MifidFillingEntity::getStatus)
                .orElse(null);

        validateStatus(currentStatus, actionType);
    }

    @Override
    public void calculateAndSetNewStatus(MifidFilling mifidFilling, UUID customerId) {
        Optional<MifidFillingEntity> mifidFillingEntityOptional = mifidFillingRepository
                .findTopByCustomerIdAndStatusNotOrderByFillingDateDesc(customerId, MifidFillingStatus.DEPRECATED);

        mifidFillingEntityOptional.ifPresentOrElse(
                mifidFillingEntity ->
                        calculateStatus(mifidFilling).ifPresent(
                                status -> {
                                    mifidFillingEntity.setStatus(status);
                                    mifidFilling.setStatus(status);
                                }),
                () -> {
                    // if no existing filling is found, we cannot calculate status
                    throw new IllegalStateException("No existing Mifid filling found for customer with ID: " + customerId);
                });
    }

    private Optional<MifidFillingStatus> calculateStatus(MifidFilling mifidFilling) {
        long expectedQuestions = mifidQuestionnaireRepository.countQuestionsByQuestionnaireId(mifidFilling.getQuestionnaireId());
        long answeredQuestions = countAnsweredQuestions(mifidFilling);

        if (answeredQuestions == 0) {
            return Optional.of(MifidFillingStatus.DRAFT);
        }

        if (answeredQuestions < expectedQuestions) {
            return Optional.of(MifidFillingStatus.DRAFT);
        }

        return Optional.of(MifidFillingStatus.COMPLETED);
    }

    private long countAnsweredQuestions(MifidFilling mifidFilling) {
        if (mifidFilling.getResponses() == null) return 0;

        return mifidFilling.getResponses().stream()
                .filter(response -> response.getAnswerOption() != null)
                .count();
    }
}
