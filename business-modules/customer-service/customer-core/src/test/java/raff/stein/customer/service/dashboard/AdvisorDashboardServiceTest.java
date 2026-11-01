package raff.stein.customer.service.dashboard;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.customer.model.bo.dashboard.AdvisorDashboardStatistics;
import raff.stein.customer.model.bo.dashboard.CustomerStatusProfileCount;
import raff.stein.customer.model.entity.customer.enumeration.CustomerStatus;
import raff.stein.customer.repository.customer.CustomerRepository;
import raff.stein.customer.service.access.CustomerAccessPolicy;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdvisorDashboardServiceTest {

    private CustomerRepository customerRepository;
    private AdvisorDashboardService service;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        service = new AdvisorDashboardService(customerRepository, new CustomerAccessPolicy(customerRepository));
        SecurityContextHolder.setContext(WMPContext.builder()
                .userId(UUID.randomUUID().toString())
                .roles(Set.of("ADVISOR"))
                .tenantId("BANK_A")
                .branchCode("BR_01")
                .build());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    void shouldAddUpTheCountsOfTheAdvisorsBranch() {
        when(customerRepository.countByStatusAndRiskProfileInBranch("BR_01")).thenReturn(List.of(
                new CustomerStatusProfileCount(CustomerStatus.ACTIVE, "Balanced", 3),
                new CustomerStatusProfileCount(CustomerStatus.ACTIVE, null, 1),
                new CustomerStatusProfileCount(CustomerStatus.DRAFT, null, 2),
                new CustomerStatusProfileCount(CustomerStatus.PENDING_VERIFICATION, "Balanced", 4),
                new CustomerStatusProfileCount(CustomerStatus.REJECTED, "Dynamic", 5)));

        final AdvisorDashboardStatistics statistics = service.getStatistics();

        assertEquals(15, statistics.getTotal());
        assertEquals(4, statistics.getActive());
        assertEquals(2, statistics.getOnboarding());
        assertEquals(4, statistics.getPendingVerification());
        assertEquals(2, statistics.getRiskProfiles().size());
        assertEquals("Balanced", statistics.getRiskProfiles().getFirst().getProfile());
        assertEquals(7, statistics.getRiskProfiles().getFirst().getCount());
    }

    @Test
    void shouldAnswerZerosForAnEmptyBranch() {
        when(customerRepository.countByStatusAndRiskProfileInBranch("BR_01")).thenReturn(List.of());

        final AdvisorDashboardStatistics statistics = service.getStatistics();

        assertEquals(0, statistics.getTotal());
        assertEquals(List.of(), statistics.getRiskProfiles());
    }
}
