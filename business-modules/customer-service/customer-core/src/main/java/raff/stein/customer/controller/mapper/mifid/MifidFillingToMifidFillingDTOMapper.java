package raff.stein.customer.controller.mapper.mifid;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.MifidFillingDTO;
import raff.stein.customer.controller.mapper.CustomerControllerCommonMapperConfig;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;

@Mapper(
        config = CustomerControllerCommonMapperConfig.class,
        uses = {
                MifidResponseToMifidResponseDTOMapper.class,
        })
public interface MifidFillingToMifidFillingDTOMapper {

    MifidFillingToMifidFillingDTOMapper MAPPER = Mappers.getMapper(MifidFillingToMifidFillingDTOMapper.class);

    MifidFillingDTO toMifidFillingDTO(MifidFilling mifidFilling);

    MifidFilling toMifidFilling(MifidFillingDTO mifidFillingDTO);
}
