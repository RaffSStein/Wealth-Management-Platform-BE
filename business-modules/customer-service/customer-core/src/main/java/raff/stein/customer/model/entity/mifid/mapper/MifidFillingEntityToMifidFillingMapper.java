package raff.stein.customer.model.entity.mifid.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.model.bo.mifid.filling.CustomerRiskProfile;
import raff.stein.customer.model.bo.mifid.filling.MifidFilling;
import raff.stein.customer.model.entity.mapper.CustomerEntityCommonMapperConfig;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;

@Mapper(
        config = CustomerEntityCommonMapperConfig.class,
        uses = {MifidResponseEntityToMifidResponseMapper.class})
public interface MifidFillingEntityToMifidFillingMapper {

    MifidFillingEntityToMifidFillingMapper MAPPER = Mappers.getMapper(MifidFillingEntityToMifidFillingMapper.class);

    @Mapping(target = "fillingId", source = "id")
    @Mapping(target = "customerRiskProfile", source = "mifidFillingEntity", qualifiedByName = "toCustomerRiskProfile")
    MifidFilling toMifidFilling(MifidFillingEntity mifidFillingEntity);

    // the risk profile is computed by the submit only, never taken from a client payload
    @Mapping(target = "responses", ignore = true)
    @Mapping(target = "calculatedRiskProfile", ignore = true)
    @Mapping(target = "riskScore", ignore = true)
    MifidFillingEntity toMifidFillingEntity(MifidFilling mifidFilling);

    // null until the filling is submitted
    @Named("toCustomerRiskProfile")
    default CustomerRiskProfile toCustomerRiskProfile(MifidFillingEntity mifidFillingEntity) {
        if (mifidFillingEntity == null || mifidFillingEntity.getCalculatedRiskProfile() == null) {
            return null;
        }
        return CustomerRiskProfile.builder()
                .profile(mifidFillingEntity.getCalculatedRiskProfile())
                .score(mifidFillingEntity.getRiskScore())
                .build();
    }
}
