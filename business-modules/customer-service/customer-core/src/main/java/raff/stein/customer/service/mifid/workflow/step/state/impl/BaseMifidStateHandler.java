package raff.stein.customer.service.mifid.workflow.step.state.impl;

import lombok.extern.slf4j.Slf4j;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.customer.repository.mifid.MifidFillingRepository;
import raff.stein.customer.repository.mifid.MifidQuestionnaireRepository;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.customer.service.mifid.workflow.step.state.MifidStateHandler;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;

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

    // currentStatus is null when the customer has no filling yet
    public abstract void validateStatus(MifidFillingStatus currentStatus, MifidActionType actionType);

    @Override
    public void validate(MifidFilling mifidFilling, UUID customerId, MifidActionType actionType) {
        final MifidFillingStatus currentStatus = findTargetFilling(mifidFilling, customerId)
                .map(MifidFillingEntity::getStatus)
                .orElse(null);
        validateStatus(currentStatus, actionType);
    }

    @Override
    public void calculateAndSetNewStatus(MifidFilling mifidFilling, UUID customerId) {
        final MifidFillingEntity mifidFillingEntity = findTargetFilling(mifidFilling, customerId)
                .orElseThrow(() -> new IllegalStateException("No existing Mifid filling found for customer with ID: " + customerId));
        final MifidFillingStatus status = calculateStatus(mifidFilling);
        mifidFillingEntity.setStatus(status);
        mifidFilling.setStatus(status);
    }

    /**
     * Resolves the filling the operation refers to: the one identified by fillingId when provided,
     * otherwise the latest non-deprecated filling of the customer.
     */
    public Optional<MifidFillingEntity> findTargetFilling(MifidFilling mifidFilling, UUID customerId) {
        if (mifidFilling != null && mifidFilling.getFillingId() != null) {
            final Long fillingId = mifidFilling.getFillingId();
            return Optional.of(mifidFillingRepository.findByIdAndCustomerId(fillingId, customerId)
                    .orElseThrow(GenericObjectNotFoundException.with(String.valueOf(fillingId))));
        }
        return mifidFillingRepository.findTopByCustomerIdAndStatusNotOrderByFillingDateDesc(customerId, MifidFillingStatus.DEPRECATED);
    }

    private MifidFillingStatus calculateStatus(MifidFilling mifidFilling) {
        final long expectedQuestions = mifidQuestionnaireRepository.countQuestionsByQuestionnaireId(mifidFilling.getQuestionnaireId());
        final long answeredQuestions = countAnsweredQuestions(mifidFilling);
        return answeredQuestions >= expectedQuestions && answeredQuestions > 0
                ? MifidFillingStatus.COMPLETED
                : MifidFillingStatus.DRAFT;
    }

    private long countAnsweredQuestions(MifidFilling mifidFilling) {
        if (mifidFilling.getResponses() == null) return 0;
        return mifidFilling.getResponses().stream()
                .filter(response -> response.getAnswerOption() != null)
                .count();
    }
}
