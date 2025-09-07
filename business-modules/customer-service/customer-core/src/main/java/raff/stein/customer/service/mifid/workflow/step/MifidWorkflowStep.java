package raff.stein.customer.service.mifid.workflow.step;

@FunctionalInterface
public interface MifidWorkflowStep {

    MifidWorkflowContext apply(MifidWorkflowContext context);
}
