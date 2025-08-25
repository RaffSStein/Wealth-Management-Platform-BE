package raff.stein.customer.model.bo.mifid.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RiskProfileConfiguration {

    private String profileCode;
    private Integer minScore;
    private Integer maxScore;
    private String profileLabel;
}
