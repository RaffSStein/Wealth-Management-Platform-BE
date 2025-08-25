package raff.stein.customer.controller.mapper.mifid;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.MifidResponseDTO;
import raff.stein.customer.controller.mapper.CustomerControllerCommonMapperConfig;
import raff.stein.customer.model.bo.mifid.filling.MifidResponse;

@Mapper(
        config = CustomerControllerCommonMapperConfig.class,
        uses = {MifidQuestionToMifidQuestionDTOMapper.class})
public interface MifidResponseToMifidResponseDTOMapper {

    MifidResponseToMifidResponseDTOMapper MAPPER = Mappers.getMapper(MifidResponseToMifidResponseDTOMapper.class);

    MifidResponseDTO toMifidResponseDTO(MifidResponse mifidResponse);

    MifidResponse toMifidResponse(MifidResponseDTO mifidResponseDTO);
}
