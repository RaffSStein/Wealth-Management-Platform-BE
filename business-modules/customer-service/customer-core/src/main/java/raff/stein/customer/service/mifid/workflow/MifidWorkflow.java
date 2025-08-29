package raff.stein.customer.service.mifid.workflow;

import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;
import raff.stein.customer.service.mifid.workflow.step.MifidWorkflowContext;
import raff.stein.customer.service.mifid.workflow.step.MifidWorkflowStep;

import java.util.List;
import java.util.UUID;

public interface MifidWorkflow {

    MifidActionType getActionType();

    List<MifidWorkflowStep> preHooks();

    MifidWorkflowStep command();

    List<MifidWorkflowStep> postHooks();

    default MifidFilling execute(UUID customerId, MifidFilling input) {
        // Initialize context
        MifidWorkflowContext context = MifidWorkflowContext
                .builder()
                .customerId(customerId)
                .inputBo(input)
                .build();
        // Execute pre-hooks
        for (MifidWorkflowStep step : preHooks()) {
            context = step.apply(context);
        }
        // Execute main command
        context = command().apply(context);
        // Execute post-hooks
        for (MifidWorkflowStep step : postHooks()) {
            context = step.apply(context);
        }
        // Return result
        return context.getResultBo();
    }
}
