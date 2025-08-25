package raff.stein.customer.model.entity.mifid.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.model.bo.mifid.config.MifidQuestionnaireConfig;
import raff.stein.customer.model.entity.mapper.CustomerEntityCommonMapperConfig;
import raff.stein.customer.model.entity.mifid.MifidQuestionnaireEntity;

@Mapper(config = CustomerEntityCommonMapperConfig.class)
public interface MifidQuestionnaireEntityToMifidQuestionnaireMapper {

    MifidQuestionnaireEntityToMifidQuestionnaireMapper MAPPER = Mappers.getMapper(MifidQuestionnaireEntityToMifidQuestionnaireMapper.class);

    @Mapping(target = "riskProfileConfigurations", source = "riskProfiles")
    MifidQuestionnaireConfig toMifidQuestionnaireConfig(MifidQuestionnaireEntity mifidQuestionnaireEntity);

    MifidQuestionnaireEntity toMifidQuestionnaireEntity(MifidQuestionnaireConfig mifidQuestionnaireConfig);
}
