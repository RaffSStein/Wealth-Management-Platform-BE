package raff.stein.customer.service.mifid.workflow.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.repository.mifid.MifidQuestionnaireRepository;
import raff.stein.customer.service.mifid.command.impl.SubmitMifidCommand;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.customer.service.mifid.workflow.MifidWorkflow;
import raff.stein.customer.service.mifid.workflow.step.MifidWorkflowStep;
import raff.stein.customer.service.mifid.workflow.step.state.impl.SubmitMifidStateHandler;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SubmitMifidWorkflow implements MifidWorkflow {

    private final SubmitMifidCommand submitCommand;
    private final SubmitMifidStateHandler submitMifidStateHandler;
    private final MifidQuestionnaireRepository mifidQuestionnaireRepository;


    @Override
    public MifidActionType getActionType() {
        return MifidActionType.SUBMIT;
    }

    @Override
    public List<MifidWorkflowStep> preHooks() {
        return List.of(
                validatePreviousStatus(),
                validateAnswers());
    }

    @Override
    public MifidWorkflowStep command() {
        return context -> {
            MifidFilling result = submitCommand.execute(context.getCustomerId(), context.getInputBo());
            context.setResultBo(result);
            return context;
        };
    }

    @Override
    public List<MifidWorkflowStep> postHooks() {
        return List.of(calculateNextStatus());
    }



    private MifidWorkflowStep validatePreviousStatus() {
        return context -> {
            submitMifidStateHandler.validate(context.getInputBo(), context.getCustomerId(), MifidActionType.SUBMIT);
            return context;
        };
    }

    private MifidWorkflowStep validateAnswers() {
        return context -> {
            MifidFilling mifidFilling = context.getInputBo();
            long expectedQuestions = mifidQuestionnaireRepository.countQuestionsByQuestionnaireId(mifidFilling.getQuestionnaireId());
            long answeredQuestions = countAnsweredQuestions(mifidFilling);
            if (answeredQuestions < expectedQuestions) {
                throw new IllegalStateException("Cannot submit MIFID filling with ID: " + mifidFilling.getFillingId() +
                        " because not all questions are answered. Answered questions: " + answeredQuestions +
                        ", expected questions: " + expectedQuestions);
            }

            return context;
        };
    }

    private MifidWorkflowStep calculateNextStatus() {
        return context -> {
            submitMifidStateHandler.calculateAndSetNewStatus(context.getInputBo(), context.getCustomerId());
            return context;
        };
    }

    private long countAnsweredQuestions(MifidFilling mifidFilling) {
        if (mifidFilling.getResponses() == null) return 0;

        return mifidFilling.getResponses().stream()
                .filter(response -> response.getAnswerOption() != null)
                .count();
    }
}
