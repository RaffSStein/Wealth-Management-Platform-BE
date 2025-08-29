package raff.stein.customer.service.aml.pipeline.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableConfigurationProperties(AmlProperties.class)
public class AmlConfiguration {

    private final AmlProperties properties;

    public AmlConfiguration(AmlProperties properties) {
        this.properties = properties;
    }

    public List<AmlProperties.StepConfig> getStepsFor(String jurisdiction) {
        var cfg = properties.getJurisdictions().get(jurisdiction);
        return cfg == null || cfg.getSteps() == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(cfg.getSteps());
    }
}
