package raff.stein.customer.model.entity.aml.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.model.bo.aml.AmlVerification;
import raff.stein.customer.model.entity.aml.AmlVerificationEntity;
import raff.stein.customer.model.entity.mapper.CustomerEntityCommonMapperConfig;

@Mapper(config = CustomerEntityCommonMapperConfig.class)
public interface AmlVerificationToAmlVerificationEntityMapper {

    AmlVerificationToAmlVerificationEntityMapper MAPPER = Mappers.getMapper(AmlVerificationToAmlVerificationEntityMapper.class);

    AmlVerification toAmlVerification(AmlVerificationEntity entity);

    AmlVerificationEntity toAmlVerificationEntity(AmlVerification amlVerification);
}
