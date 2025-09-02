package raff.stein.customer.service.aml.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import raff.stein.customer.service.aml.pipeline.step.AmlContext;
import raff.stein.customer.service.aml.pipeline.step.AmlStep;
import raff.stein.customer.service.aml.pipeline.step.AmlStepResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmlPipelineExecutor {

    private final AmlPipelineFactory pipelineFactory;

    public AmlResult execute(AmlContext context) {
        Instant startedAt = Instant.now();
        List<AmlStep> pipeline = pipelineFactory.build(context.getJurisdiction());
        List<AmlStepResult> executed = new ArrayList<>();

        boolean anyReview = false;
        boolean anyPending = false;
        AmlStepResult.StepStatus overall = AmlStepResult.StepStatus.PASSED;

        for (AmlStep step : pipeline) {
            log.info("Executing AML step: {} for case {}", step.name(), context.getAmlCaseId());
            AmlStepResult result = step.execute(context);
            executed.add(result);
            log.info("Completed AML step: {} for case {} with result: {}",
                    step.name(),
                    context.getAmlCaseId(),
                    result.status());

            // Precedence: FAILED > WAITING_EXTERNAL > REVIEW > PASSED
            if (result.status() == AmlStepResult.StepStatus.FAILED) {
                overall = AmlStepResult.StepStatus.FAILED;
                log.error("AML step {} failed for case {}. Stopping pipeline.", step.name(), context.getAmlCaseId());
                break;
            } else if (result.status() == AmlStepResult.StepStatus.WAITING_EXTERNAL) {
                anyPending = true;
            } else if (result.status() == AmlStepResult.StepStatus.REVIEW) {
                anyReview = true;
            }
        }

        // Determine overall status if not already FAILED
        if (overall != AmlStepResult.StepStatus.FAILED) {
            if (anyPending) {
                overall = AmlStepResult.StepStatus.WAITING_EXTERNAL;
            } else if (anyReview) {
                overall = AmlStepResult.StepStatus.REVIEW;
            }
        }

        return AmlResult.builder()
                .amlCaseId(context.getAmlCaseId())
                .jurisdiction(context.getJurisdiction())
                .executedSteps(executed)
                .overallStatus(overall)
                .startedAt(startedAt)
                .finishedAt(Instant.now())
                .build();
    }
}
