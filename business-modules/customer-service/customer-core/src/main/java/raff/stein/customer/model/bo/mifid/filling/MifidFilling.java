package raff.stein.customer.model.bo.mifid.filling;

import lombok.Builder;
import lombok.Data;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class MifidFilling {

    private Long questionnaireId;
    private Long fillingId;
    private LocalDate fillingDate;
    private boolean isValid;
    private MifidFillingStatus status;
    private List<MifidResponse> responses;
    private CustomerRiskProfile customerRiskProfile;

}
