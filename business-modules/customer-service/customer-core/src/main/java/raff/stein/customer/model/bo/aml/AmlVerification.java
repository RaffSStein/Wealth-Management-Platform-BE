package raff.stein.customer.model.bo.aml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import raff.stein.customer.model.bo.customer.Customer;
import raff.stein.customer.service.aml.pipeline.AmlResult;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmlVerification {
    // Database identifier (optional in BO, useful for reads/updates)
    private Long id;
    // Business correlation id for the AML case
    private UUID amlCaseId;
    // Non-persistent execution details from the AML pipeline (read-model convenience)
    private AmlResult amlResult;
    // Owning customer details (read-model convenience)
    private Customer customer;
    // Jurisdiction used for the AML pipeline (e.g., EU, UK, USA)
    private String jurisdiction;
    // When the verification was performed
    private OffsetDateTime verificationDate;
    // Current verification status (e.g., PASSED/FAILED/REVIEW/PENDING)
    private String status;
    // External provider info (if any)
    private String providerName;
    // ISO 3166-1 alpha-2 of country used in screening
    private String countryCode;
    // Provider/engine summarized result
    private String verificationResult;
    // Computed AML risk score
    private Double riskScore;
    // Free-form operational notes
    private String notes;
    // When the verification expires
    private OffsetDateTime expiresAt;


}
