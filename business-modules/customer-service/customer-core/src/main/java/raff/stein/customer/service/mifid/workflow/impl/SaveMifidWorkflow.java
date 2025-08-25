package raff.stein.customer.service.mifid.workflow.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.service.mifid.command.impl.SaveMifidCommand;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.customer.service.mifid.workflow.MifidWorkflow;
import raff.stein.customer.service.mifid.workflow.step.MifidWorkflowStep;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SaveMifidWorkflow implements MifidWorkflow {

    private final SaveMifidCommand saveCommand;

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
        //TODO: implement
        return List.of();
    }
}
