package raff.stein.customer.model.bo.financial;

import lombok.Builder;
import lombok.Data;
import raff.stein.customer.model.entity.financial.enumeration.FinancialTypeEnum;

@Data
@Builder
public class FinancialType {

    private String name;
    private FinancialTypeEnum type;
    private String description;
}
