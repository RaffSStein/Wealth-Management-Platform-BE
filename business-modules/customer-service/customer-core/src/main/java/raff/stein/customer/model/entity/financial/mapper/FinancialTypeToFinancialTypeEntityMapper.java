package raff.stein.customer.model.entity.financial.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import raff.stein.customer.model.bo.financial.FinancialType;
import raff.stein.customer.model.entity.financial.FinancialTypeEntity;
import raff.stein.customer.model.entity.mapper.CustomerEntityCommonMapperConfig;

@Mapper(config = CustomerEntityCommonMapperConfig.class)
public interface FinancialTypeToFinancialTypeEntityMapper {

    FinancialTypeToFinancialTypeEntityMapper MAPPER = Mappers.getMapper(FinancialTypeToFinancialTypeEntityMapper.class);

    FinancialTypeEntity toFinancialTypeEntity(FinancialType financialType);

    FinancialType toFinancialType(FinancialTypeEntity financialTypeEntity);
}
