package raff.stein.customer.service.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.customer.model.bo.dashboard.AdvisorDashboardStatistics;
import raff.stein.customer.model.bo.dashboard.CustomerStatusProfileCount;
import raff.stein.customer.model.entity.customer.enumeration.CustomerStatus;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.service.access.CustomerAccessPolicy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdvisorDashboardService {

    private final CustomerRepository customerRepository;
    private final CustomerAccessPolicy customerAccessPolicy;

    /**
     * The counts of the customers in the advisor's active branch. Who may call it is checked before the controller.
     */
    @Transactional(readOnly = true)
    public AdvisorDashboardStatistics getStatistics() {
        final String branchCode = customerAccessPolicy.branchScope()
                .orElseThrow(() -> new IllegalStateException("The advisor dashboard needs a branch"));
        final List<CustomerStatusProfileCount> counts = customerRepository.countByStatusAndRiskProfileInBranch(branchCode);
        final Map<String, Long> byProfile = counts.stream()
                .filter(count -> count.riskProfile() != null)
                .collect(Collectors.groupingBy(
                        CustomerStatusProfileCount::riskProfile,
                        Collectors.summingLong(CustomerStatusProfileCount::customers)));
        return AdvisorDashboardStatistics.builder()
                .total(counts.stream().mapToLong(CustomerStatusProfileCount::customers).sum())
                .active(countOf(counts, CustomerStatus.ACTIVE))
                .onboarding(countOf(counts, CustomerStatus.DRAFT))
                .pendingVerification(countOf(counts, CustomerStatus.PENDING_VERIFICATION))
                .riskProfiles(byProfile.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> AdvisorDashboardStatistics.RiskProfileCount.builder()
                                .profile(entry.getKey())
                                .count(entry.getValue())
                                .build())
                        .toList())
                .build();
    }

    private static long countOf(List<CustomerStatusProfileCount> counts, CustomerStatus status) {
        return counts.stream()
                .filter(count -> count.status() == status)
                .mapToLong(CustomerStatusProfileCount::customers)
                .sum();
    }
}
