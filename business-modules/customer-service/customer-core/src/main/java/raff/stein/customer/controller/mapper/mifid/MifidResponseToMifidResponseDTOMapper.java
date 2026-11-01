package raff.stein.customer.controller.mapper.mifid;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import io.openapiprocessor.openapi.model.MifidResponseDto;
import raff.stein.customer.controller.mapper.CustomerControllerCommonMapperConfig;
import raff.stein.customer.model.bo.mifid.filling.MifidResponse;

@Mapper(
        config = CustomerControllerCommonMapperConfig.class,
        uses = {MifidQuestionToMifidQuestionDTOMapper.class})
public interface MifidResponseToMifidResponseDTOMapper {

    MifidResponseToMifidResponseDTOMapper MAPPER = Mappers.getMapper(MifidResponseToMifidResponseDTOMapper.class);

    MifidResponseDto toMifidResponseDTO(MifidResponse mifidResponse);

    MifidResponse toMifidResponse(MifidResponseDto mifidResponseDTO);
}
