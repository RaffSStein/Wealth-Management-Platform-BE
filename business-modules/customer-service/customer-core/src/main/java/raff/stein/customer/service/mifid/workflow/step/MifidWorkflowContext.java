package raff.stein.customer.service.mifid.workflow.step;

import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import org.openapitools.model.MifidFillingDTO;
import raff.stein.customer.controller.mapper.mifid.MifidFillingToMifidFillingDTOMapper;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;

import java.util.UUID;

@Data
@Builder
public class MifidWorkflowContext {

    private UUID customerId;
    private MifidFilling inputBo;
    private MifidFilling resultBo;

}
