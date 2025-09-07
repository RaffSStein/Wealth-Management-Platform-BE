package raff.stein.customer.service.aml.pipeline.step;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record AmlStepResult(
        String stepName,
        StepStatus status,
        Decision decision,
        String reason,
        Map<String,Object> details,
        Instant startedAt,
        Instant finishedAt) {

    public enum StepStatus {
        PASSED,
        FAILED,
        REVIEW,
        WAITING_EXTERNAL
    }

    public enum Decision {
        PASS,
        FAIL,
        MANUAL_REVIEW,
        PENDING
    }
}
