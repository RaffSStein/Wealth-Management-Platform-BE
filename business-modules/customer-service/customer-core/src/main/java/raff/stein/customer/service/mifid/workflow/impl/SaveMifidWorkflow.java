package raff.stein.customer.service.mifid.workflow.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.customer.service.mifid.command.impl.SaveMifidCommand;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.customer.service.mifid.workflow.MifidWorkflow;
import raff.stein.customer.service.mifid.workflow.step.MifidWorkflowStep;
import raff.stein.customer.service.mifid.workflow.step.state.impl.UpdateMifidStateHandler;
import raff.stein.customer.service.onboarding.OnboardingService;
import raff.stein.customer.service.onboarding.handler.OnboardingStepContext;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SaveMifidWorkflow implements MifidWorkflow {

    private final SaveMifidCommand saveCommand;
    private final OnboardingService onboardingService;
    // status derivation is the same for save and update
    private final UpdateMifidStateHandler updateMifidStateHandler;

    @Override
    public MifidActionType getActionType() {
        return MifidActionType.SAVE;
    }

    @Override
    public List<MifidWorkflowStep> preHooks() {
        return List.of();
    }

    @Override
    public MifidWorkflowStep command() {
        return context -> {
            MifidFilling result = saveCommand.execute(context.getCustomerId(), context.getInputBo());
            context.setResultBo(result);
            return context;
        };
    }

    @Override
    public List<MifidWorkflowStep> postHooks() {
        return List.of(
                calculateStatus(),                  // DRAFT or COMPLETED, derived from the answers
                updateOnboardingStatus());
    }

    private MifidWorkflowStep calculateStatus() {
        return context -> {
            if (context.getResultBo() != null) {
                updateMifidStateHandler.calculateAndSetNewStatus(context.getResultBo(), context.getCustomerId());
            }
            return context;
        };
    }

    private MifidWorkflowStep updateOnboardingStatus() {
        return context -> {
            // call for initiation of the MIFID onboarding step
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
}
