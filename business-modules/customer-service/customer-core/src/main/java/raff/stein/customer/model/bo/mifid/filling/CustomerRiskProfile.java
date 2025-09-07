package raff.stein.customer.model.bo.mifid.filling;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerRiskProfile {

    private String profile;
    private Integer score;

}
