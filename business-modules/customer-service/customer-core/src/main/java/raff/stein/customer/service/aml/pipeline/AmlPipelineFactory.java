package raff.stein.customer.service.aml.pipeline;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import raff.stein.customer.service.aml.pipeline.config.AmlConfiguration;
import raff.stein.customer.service.aml.pipeline.config.AmlProperties;
import raff.stein.customer.service.aml.pipeline.step.AmlStep;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AmlPipelineFactory {

    private final AmlConfiguration config;
    private final Map<String, AmlStep> stepMap;

    public AmlPipelineFactory(AmlConfiguration config, List<AmlStep> allSteps) {
        this.config = config;
        this.stepMap = allSteps
                .stream()
                .collect(Collectors.toMap(AmlStep::name, Function.identity()));
    }

    public List<AmlStep> build(@NonNull String jurisdiction) {
        final List<AmlProperties.StepConfig> stepConfigs = config.getStepsFor(jurisdiction);
        return stepConfigs
                .stream()
                .filter(sc -> sc.getEnabled() != null && sc.getEnabled())
                // step name is prefixed with jurisdiction to allow same step in different jurisdictions
                // e.g. "ue-sanctions-screening-step"
                .map(sc -> stepMap.get(jurisdiction + "-" + sc.getName()))
                .filter(Objects::nonNull)
                .toList();
    }

}
