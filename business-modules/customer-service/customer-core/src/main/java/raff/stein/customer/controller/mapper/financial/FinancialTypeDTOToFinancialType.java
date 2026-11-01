package raff.stein.customer.controller.mapper.financial;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import io.openapiprocessor.openapi.model.FinancialTypeDto;
import raff.stein.customer.controller.mapper.CustomerControllerCommonMapperConfig;
import raff.stein.customer.model.bo.financial.FinancialType;

@Mapper(config = CustomerControllerCommonMapperConfig.class)
public interface FinancialTypeDTOToFinancialType {

    FinancialTypeDTOToFinancialType MAPPER = Mappers.getMapper(FinancialTypeDTOToFinancialType.class);

    FinancialTypeDto toFinancialTypeDTO(FinancialType financialType);

    FinancialType toFinancialType(FinancialTypeDto financialTypeDTO);

}
