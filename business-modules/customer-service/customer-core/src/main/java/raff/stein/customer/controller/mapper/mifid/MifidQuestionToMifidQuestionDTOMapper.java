package raff.stein.customer.controller.mapper.mifid;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import io.openapiprocessor.openapi.model.MifidQuestionDto;
import raff.stein.customer.controller.mapper.CustomerControllerCommonMapperConfig;
import raff.stein.customer.model.bo.mifid.config.MifidQuestion;

@Mapper(config = CustomerControllerCommonMapperConfig.class)
public interface MifidQuestionToMifidQuestionDTOMapper {

    MifidQuestionToMifidQuestionDTOMapper MAPPER = Mappers.getMapper(MifidQuestionToMifidQuestionDTOMapper.class);

    // ignore answerOptions for filling because they are not needed
    @Mapping(target = "answerOptions", ignore = true)
    MifidQuestionDto toMifidQuestionDTO(MifidQuestion mifidQuestion);

    @Mapping(target = "answerOptions", ignore = true)
    MifidQuestion toMifidQuestion(MifidQuestionDto mifidQuestionDTO);
}
