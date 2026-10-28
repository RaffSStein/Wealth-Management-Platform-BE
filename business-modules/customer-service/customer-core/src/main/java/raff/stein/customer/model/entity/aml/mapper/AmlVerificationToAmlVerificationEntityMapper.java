package raff.stein.customer.model.entity.aml.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.model.bo.aml.AmlVerification;
import raff.stein.customer.model.entity.aml.AmlVerificationEntity;
import raff.stein.customer.model.entity.mapper.CustomerEntityCommonMapperConfig;

@Mapper(config = CustomerEntityCommonMapperConfig.class)
public interface AmlVerificationToAmlVerificationEntityMapper {

    AmlVerificationToAmlVerificationEntityMapper MAPPER = Mappers.getMapper(AmlVerificationToAmlVerificationEntityMapper.class);

    // the customer risk profile is irrelevant to AML and is owned by the MiFID submit
    @Mapping(target = "customer.riskProfile", ignore = true)
    AmlVerification toAmlVerification(AmlVerificationEntity entity);

    @Mapping(target = "customer.riskProfile", ignore = true)
    @Mapping(target = "customer.riskScore", ignore = true)
    AmlVerificationEntity toAmlVerificationEntity(AmlVerification amlVerification);
}
