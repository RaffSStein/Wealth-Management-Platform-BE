package raff.stein.customer.model.entity.mifid.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.model.bo.mifid.config.MifidQuestion;
import raff.stein.customer.model.entity.mapper.CustomerEntityCommonMapperConfig;
import raff.stein.customer.model.entity.mifid.MifidQuestionEntity;

@Mapper(config = CustomerEntityCommonMapperConfig.class)
public interface MifidQuestionEntityToMifidQuestionMapper {

    MifidQuestionEntityToMifidQuestionMapper MAPPER = Mappers.getMapper(MifidQuestionEntityToMifidQuestionMapper.class);

    @Mapping(target = "answerOptions", ignore = true)
    MifidQuestion toMifidQuestion(MifidQuestionEntity mifidQuestionEntity);

    @Mapping(target = "answerOptions", ignore = true)
    MifidQuestionEntity toMifidQuestionEntity(MifidQuestion mifidQuestion);

}
