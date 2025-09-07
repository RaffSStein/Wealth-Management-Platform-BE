package raff.stein.customer.service.aml.pipeline;

import lombok.Builder;
import raff.stein.customer.service.aml.pipeline.step.AmlStepResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record AmlResult(
        UUID amlCaseId,
        String jurisdiction,
        List<AmlStepResult> executedSteps,
        AmlStepResult.StepStatus overallStatus,
        Instant startedAt,
        Instant finishedAt
) {}
