package raff.stein.customer.service.mifid.workflow.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.service.mifid.command.impl.UpdateMifidCommand;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.customer.service.mifid.workflow.MifidWorkflow;
import raff.stein.customer.service.mifid.workflow.step.MifidWorkflowStep;
import raff.stein.customer.service.mifid.workflow.step.state.impl.UpdateMifidStateHandler;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UpdateMifidWorkflow implements MifidWorkflow {

    private final UpdateMifidCommand updateCommand;
    private final UpdateMifidStateHandler updateMifidStateHandler;

    @Override
    public MifidActionType getActionType() {
        return MifidActionType.UPDATE;
    }

    @Override
    public List<MifidWorkflowStep> preHooks() {
        // on preHook, we validate the current state allows the update operation
        return List.of(validateStep());
    }

    @Override
    public MifidWorkflowStep command() {
        return context -> {
            MifidFilling result = updateCommand.execute(context.getCustomerId(), context.getInputBo());
            context.setResultBo(result);
            return context;
        };
    }

    @Override
    public List<MifidWorkflowStep> postHooks() {
        return List.of(calculateStatusStep());
    }

    private MifidWorkflowStep validateStep() {
        return context -> {
            updateMifidStateHandler.validate(context.getInputBo(), context.getCustomerId(), MifidActionType.UPDATE);
            return context;
        };
    }

    private MifidWorkflowStep calculateStatusStep() {
        return context -> {
            updateMifidStateHandler.calculateAndSetNewStatus(context.getInputBo(), context.getCustomerId());
            return context;
        };
    }

}
