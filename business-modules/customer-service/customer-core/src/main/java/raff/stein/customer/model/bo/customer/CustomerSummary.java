package raff.stein.customer.model.bo.customer;

import lombok.Builder;
import lombok.Data;
import raff.stein.customer.model.bo.mifid.filling.CustomerRiskProfile;

import java.util.UUID;

/**
 * A row of the customer list, not the full record.
 */
@Data
@Builder
public class CustomerSummary {

    private UUID id;
    private String firstName;
    private String lastName;
    private String companyName;
    private String taxId;
    private String customerType;
    private String customerStatus;
    private CustomerRiskProfile riskProfile;
}
