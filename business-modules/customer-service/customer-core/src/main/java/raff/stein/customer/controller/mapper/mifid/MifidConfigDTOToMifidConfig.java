package raff.stein.customer.controller.mapper.mifid;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.MifidQuestionnaireConfigDTO;
import raff.stein.customer.controller.mapper.CustomerControllerCommonMapperConfig;
import raff.stein.customer.model.bo.mifid.config.MifidQuestionnaireConfig;

@Mapper(config = CustomerControllerCommonMapperConfig.class)
public interface MifidConfigDTOToMifidConfig {

    MifidConfigDTOToMifidConfig MAPPER = Mappers.getMapper(MifidConfigDTOToMifidConfig.class);

    MifidQuestionnaireConfig toMifidConfig(MifidQuestionnaireConfigDTO mifidQuestionnaireConfigDTO);

    @Mapping(target = "questionnaireId", source = "id")
    MifidQuestionnaireConfigDTO toMifidConfigDTO(MifidQuestionnaireConfig mifidQuestionnaireConfig);

}
