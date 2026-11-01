package raff.stein.customer.model.bo.dashboard;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdvisorDashboardStatistics {

    private long total;
    private long active;
    private long onboarding;
    private long pendingVerification;
    private List<RiskProfileCount> riskProfiles;

    @Data
    @Builder
    public static class RiskProfileCount {
        private String profile;
        private long count;
    }
}
