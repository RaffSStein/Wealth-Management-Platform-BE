package raff.stein.customer.service.aml.pipeline.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "aml")
public class AmlProperties {

    private Map<String, JurisdictionConfig> jurisdictions;

    @Data
    public static class JurisdictionConfig {

        private List<StepConfig> steps;
        private Thresholds thresholds;
        private Map<String, String> stepConditions;
    }

    @Data
    public static class StepConfig {
        private String name;
        private Boolean enabled;
    }

    @Data
    public static class Thresholds {

        private Integer eddRiskScore;
        private List<String> highRiskCountries;
    }
}
