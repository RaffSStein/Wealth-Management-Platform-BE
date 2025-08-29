package raff.stein.customer.service.aml.pipeline;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import raff.stein.customer.service.aml.pipeline.step.AmlContext;
import raff.stein.customer.service.aml.pipeline.step.AmlStep;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AmlPipelineExecutor {

    private final AmlPipelineFactory pipelineFactory;

    public void execute(AmlContext context) {
        List<AmlStep> pipeline = pipelineFactory.build(context.getJurisdiction());
        for (AmlStep step : pipeline) {
            step.execute(context);
        }
    }
}
