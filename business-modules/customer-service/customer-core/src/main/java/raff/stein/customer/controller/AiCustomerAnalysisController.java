package raff.stein.customer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import raff.stein.customer.ai.CustomerInsightAgent;

import java.util.UUID;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiCustomerAnalysisController {

    private final CustomerInsightAgent customerInsightAgent;

    public record CustomerEligibilityAnalysisResponse(
            UUID customerId,
            String isin,
            CustomerInsightAgent.EligibilityResult analysis
    ) {
    }

    @GetMapping("/analyze/{customerId}")
    public ResponseEntity<CustomerEligibilityAnalysisResponse> analyze(
            @PathVariable UUID customerId,
            @RequestParam String isin) {
        CustomerInsightAgent.EligibilityResult eligibilityResult = customerInsightAgent.analyzeEligibility(customerId, isin);
        return ResponseEntity.ok(new CustomerEligibilityAnalysisResponse(customerId, isin, eligibilityResult));
    }
}

