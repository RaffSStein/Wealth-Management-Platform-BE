package raff.stein.customer.service.mifid.workflow.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import raff.stein.customer.event.producer.CustomerOnboardedEventPublisher;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.repository.mifid.MifidQuestionnaireRepository;
import raff.stein.customer.service.mifid.command.impl.SubmitMifidCommand;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.customer.service.mifid.workflow.MifidWorkflow;
import raff.stein.customer.service.mifid.workflow.step.MifidWorkflowStep;
import raff.stein.customer.service.mifid.workflow.step.state.impl.SubmitMifidStateHandler;
import raff.stein.customer.service.onboarding.OnboardingService;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SubmitMifidWorkflow implements MifidWorkflow {

    private final SubmitMifidCommand submitCommand;
    private final SubmitMifidStateHandler submitMifidStateHandler;
    private final MifidQuestionnaireRepository mifidQuestionnaireRepository;
    private final OnboardingService onboardingService;
    private final CustomerOnboardedEventPublisher customerOnboardedEventPublisher;


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
        return List.of(
                calculateNextStatus(),              // set and save status on MifidFilling
                updateOnboardingStatus(),           // update onboarding step status
                publishCustomerOnboardedEvent());
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

    private MifidWorkflowStep updateOnboardingStatus() {
        return context -> {
            // call for submit (end) of the MIFID onboarding step
            onboardingService.proceedToStep(
                    OnboardingStep.MIFID,
                    OnboardingStepContext.builder()
                            .customerId(context.getCustomerId())
                            .metadata(Map.of(
                                    "mifidStatus", context.getResultBo().getStatus().name()
                            ))
                            .build());
            return context;
        };
    }

    private MifidWorkflowStep publishCustomerOnboardedEvent() {
        return context -> {
            customerOnboardedEventPublisher.publishCustomerOnboardedEvent(
                    context.getResultBo().getCustomerRiskProfile(),
                    context.getCustomerId());
            return context;
        };
    }
}
