package raff.stein.customer.model.bo.mifid.config;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MifidQuestionnaireConfig {

    private Long id;
    private String name;
    private String description;
    private String questionnaireVersion;
    private String status;
    private List<MifidSection> sections;
    private List<RiskProfileConfiguration> riskProfileConfigurations;
}

