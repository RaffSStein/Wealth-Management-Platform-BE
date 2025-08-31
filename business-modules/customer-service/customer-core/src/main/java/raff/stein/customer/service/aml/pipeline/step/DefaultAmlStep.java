package raff.stein.customer.service.aml.pipeline.step;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Template base for AML (Anti Money Laundering) pipeline steps.
 * <p>
 * Implements the Template Method pattern providing a stable execution flow:
 * <ol>
 *   <li>Capture start timestamp</li>
 *   <li>{@link #preValidate(AmlContext)} – pre-conditions and input checks</li>
 *   <li>{@link #doExecute(AmlContext)} – core step logic implemented by subclasses</li>
 *   <li>{@link #postProcess(AmlContext, StepOutcome)} – side-effects (e.g. persistence, events)</li>
 *   <li>Build an immutable {@link AmlStepResult} with outcome and timings</li>
 * </ol>
 * Any unhandled exception is caught and converted to a FAILED {@link AmlStepResult}
 * enriched with exception details (type and message), ensuring the pipeline can
 * continue or fail deterministically without leaking exceptions.
 * <p>
 * Subclasses must implement the abstract hooks to provide behavior while keeping
 * a consistent orchestration and result shape.
 */
public abstract class DefaultAmlStep implements AmlStep {

    /**
     * Executes the step lifecycle and returns a structured, immutable result.
     * The method is exception-safe: any thrown exception becomes a FAILED result
     * with diagnostic information in {@code details}.
     *
     * @param context shared pipeline context carrying input and cross-step data
     * @return the {@link AmlStepResult} describing status, decision, reason, details and timings
     */
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

    /**
     * Hook for validating the context before execution.
     * Implementations should throw explicit exceptions on invalid input to make
     * failures self-descriptive in the resulting {@link AmlStepResult}.
     * This method should avoid side effects.
     *
     * @param amlContext the shared AML context
     */
    public abstract void preValidate(AmlContext amlContext);

    /**
     * Hook called after successful core execution to perform side effects
     * such as persistence, emitting domain events, metrics, etc.
     * Implementations should be idempotent when possible.
     *
     * @param amlContext     the shared AML context
     * @param outcome the computed outcome from {@link #doExecute(AmlContext)}
     */
    public abstract void postProcess(AmlContext amlContext, StepOutcome outcome);

    /**
     * Core step logic. It must not catch and swallow exceptions silently.
     * Return a {@link StepOutcome} indicating the business status and decision.
     *
     * @param amlContext the shared AML context
     * @return the outcome used to build the final {@link AmlStepResult}
     */
    public abstract StepOutcome doExecute(AmlContext amlContext);

    /**
     * Immutable value describing the business outcome produced by {@link #doExecute(AmlContext)}.
     * Factory helpers are provided to create outcomes with standard combinations of
     * status and decision, while allowing a business reason and an optional details map
     * (e.g., for audit or troubleshooting).
     */
    public record StepOutcome(
            AmlStepResult.StepStatus status,
            AmlStepResult.Decision decision,
            String reason,
            Map<String, Object> details) {

        /**
         * Convenience factory for a passing outcome (no issues detected).
         * @param reason short human-readable explanation
         * @param detailsMap optional details (may be null)
         */
        public static StepOutcome pass(String reason, Map<String, Object> detailsMap) {
            return new StepOutcome(
                    AmlStepResult.StepStatus.PASSED,
                    AmlStepResult.Decision.PASS,
                    reason,
                    detailsMap);
        }

        /**
         * Convenience factory for a failing outcome (blocking condition).
         * @param reason short human-readable explanation
         * @param detailsMap optional details (may be null)
         */
        public static StepOutcome fail(String reason, Map<String, Object> detailsMap) {
            return new StepOutcome(
                    AmlStepResult.StepStatus.FAILED,
                    AmlStepResult.Decision.FAIL,
                    reason,
                    detailsMap);
        }

        /**
         * Convenience factory for a manual review outcome (non-deterministic, needs human action).
         * @param reason short human-readable explanation
         * @param detailsMap optional details (may be null)
         */
        public static StepOutcome review(String reason, Map<String, Object> detailsMap) {
            return new StepOutcome(
                    AmlStepResult.StepStatus.REVIEW,
                    AmlStepResult.Decision.MANUAL_REVIEW,
                    reason,
                    detailsMap);
        }

        /**
         * Convenience factory for a pending outcome (awaiting external system response).
         * @param reason short human-readable explanation
         * @param detailsMap optional details (may be null)
         */
        public static StepOutcome pending(String reason, Map<String, Object> detailsMap) {
            return new StepOutcome(
                    AmlStepResult.StepStatus.WAITING_EXTERNAL,
                    AmlStepResult.Decision.PENDING,
                    reason,
                    detailsMap);
        }
    }
}
