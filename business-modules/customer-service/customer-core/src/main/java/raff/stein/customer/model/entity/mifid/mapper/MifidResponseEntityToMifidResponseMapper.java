package raff.stein.customer.model.entity.mifid.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.model.bo.mifid.filling.MifidResponse;
import raff.stein.customer.model.entity.mapper.CustomerEntityCommonMapperConfig;
import raff.stein.customer.model.entity.mifid.MifidResponseEntity;

@Mapper(
        config = CustomerEntityCommonMapperConfig.class,
        uses = {MifidQuestionEntityToMifidQuestionMapper.class}
)
public interface MifidResponseEntityToMifidResponseMapper {

    MifidResponseEntityToMifidResponseMapper MAPPER = Mappers.getMapper(MifidResponseEntityToMifidResponseMapper.class);

    MifidResponse toMifidResponse(MifidResponseEntity mifidResponseEntity);

    @Mapping(target = "answerOptionId", source = "answerOption.id")
    @Mapping(target = "questionId", source = "question.id")
    @Mapping(target = "answerOption", ignore = true)
    @Mapping(target = "question", ignore = true)
    MifidResponseEntity toMifidResponseEntity(MifidResponse mifidResponse);
}
