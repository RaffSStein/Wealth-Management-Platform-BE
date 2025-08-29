package raff.stein.customer.service.aml.pipeline.step.impl;

import raff.stein.customer.service.aml.pipeline.step.AmlContext;
import raff.stein.customer.service.aml.pipeline.step.AmlStep;
import raff.stein.customer.service.aml.pipeline.step.AmlStepResult;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public abstract class DefaultAmlStep implements AmlStep {

    @Override
    public AmlStepResult execute(AmlContext context) {
        Instant start = Instant.now();
        try {
            preValidate(context);
            StepOutcome outcome = doExecute(context);
            postProcess(context, outcome);
            return AmlStepResult.builder()
                    .stepName(name())
                    .status(outcome.status())
                    .decision(outcome.decision())
                    .reason(outcome.reason())
                    .details(outcome.details())
                    .startedAt(start)
                    .finishedAt(Instant.now())
                    .build();
        } catch (Exception ex) {
            Map<String, Object> exceptionDetailsMap = new HashMap<>();
            exceptionDetailsMap.put("exception", ex.getClass().getSimpleName());
            exceptionDetailsMap.put("message", ex.getMessage());
            return AmlStepResult.builder()
                    .stepName(name())
                    .status(AmlStepResult.StepStatus.FAILED)
                    .decision(AmlStepResult.Decision.FAIL)
                    .reason("EXCEPTION:" + ex.getMessage())
                    .details(exceptionDetailsMap)
                    .startedAt(start)
                    .finishedAt(Instant.now())
                    .build();
        }
    }

    protected abstract void preValidate(AmlContext ctx);

    protected abstract void postProcess(AmlContext ctx, StepOutcome outcome);

    protected abstract StepOutcome doExecute(AmlContext ctx);

    protected record StepOutcome(
            AmlStepResult.StepStatus status,
            AmlStepResult.Decision decision,
            String reason,
            Map<String, Object> details) {

        static StepOutcome pass(String reason, Map<String, Object> d) {
            return new StepOutcome(
                    AmlStepResult.StepStatus.PASSED,
                    AmlStepResult.Decision.PASS,
                    reason,
                    d);
        }

        static StepOutcome fail(String reason, Map<String, Object> d) {
            return new StepOutcome(
                    AmlStepResult.StepStatus.FAILED,
                    AmlStepResult.Decision.FAIL,
                    reason,
                    d);
        }

        static StepOutcome review(String reason, Map<String, Object> d) {
            return new StepOutcome(
                    AmlStepResult.StepStatus.REVIEW,
                    AmlStepResult.Decision.MANUAL_REVIEW,
                    reason,
                    d);
        }

        static StepOutcome pending(String reason, Map<String, Object> d) {
            return new StepOutcome(
                    AmlStepResult.StepStatus.WAITING_EXTERNAL,
                    AmlStepResult.Decision.PENDING,
                    reason,
                    d);
        }
    }
}
