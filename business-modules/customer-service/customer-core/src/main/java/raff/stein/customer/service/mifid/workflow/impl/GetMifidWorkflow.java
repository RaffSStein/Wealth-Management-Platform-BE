package raff.stein.customer.service.mifid.workflow.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.service.mifid.command.impl.GetMifidCommand;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.customer.service.mifid.workflow.MifidWorkflow;
import raff.stein.customer.service.mifid.workflow.step.MifidWorkflowStep;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetMifidWorkflow implements MifidWorkflow {

    private final GetMifidCommand getCommand;

    @Override
    public MifidActionType getActionType() {
        return MifidActionType.GET;
    }

    @Override
    public List<MifidWorkflowStep> preHooks() {
        return List.of();
    }

    @Override
    public MifidWorkflowStep command() {
        return context -> {
            MifidFilling result = getCommand.execute(context.getCustomerId(), context.getInputBo());
            context.setResultBo(result);
            return context;
        };
    }

    @Override
    public List<MifidWorkflowStep> postHooks() {
        return List.of();
    }
}
