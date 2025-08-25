package raff.stein.customer.service.mifid.workflow;

import org.springframework.stereotype.Component;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.service.mifid.enumeration.MifidActionType;

import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

@Component
public class MifidWorkflowDispatcher {

    private final EnumMap<MifidActionType, MifidWorkflow> workflowMap;

    public MifidWorkflowDispatcher(List<MifidWorkflow> workflows) {
        this.workflowMap = new EnumMap<>(MifidActionType.class);
        workflows.forEach(workflow -> this.workflowMap.put(workflow.getActionType(), workflow));
    }

    public MifidFilling dispatch(
            MifidActionType actionType,
            UUID customerId,
            MifidFilling mifidFillingFromFE) {
        MifidWorkflow workflow = workflowMap.get(actionType);
        if (workflow == null) {
            throw new UnsupportedOperationException("Unsupported action type: " + actionType);
        }
        return workflow.execute(customerId, mifidFillingFromFE);
    }
}
