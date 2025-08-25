package raff.stein.customer.model.bo.customer;

import lombok.Builder;
import lombok.Data;
import raff.stein.customer.model.bo.financial.FinancialType;

import java.math.BigDecimal;

@Data
@Builder
public class CustomerFinancials {

    private Long id;
    private FinancialType financialType;
    private BigDecimal amount;
    private String description;

}
