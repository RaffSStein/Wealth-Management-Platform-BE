package raff.stein.bank.controller.mapper;

import io.openapiprocessor.openapi.model.BankBranchDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Pageable;
import raff.stein.bank.model.bo.BankBranch;
import raff.stein.bank.model.bo.BankSearchRequest;

@Mapper(config = BankControllerCommonMapperConfig.class)
public interface BankBranchDtoToBankBranchMapper {

    BankBranchDtoToBankBranchMapper MAPPER = Mappers.getMapper(BankBranchDtoToBankBranchMapper.class);

    BankBranchDto toBankBranchDto(BankBranch bankBranch);

    BankBranch toBankBranch(BankBranchDto bankBranchDto);

    // Search request mapping
    BankSearchRequest toBankSearchRequest(
            Pageable pageable,
            String bankCode,
            String bankName,
            String branchCode,
            String swiftCode,
            String countryCode,
            String bankType,
            String branchCity,
            String zipCode);




}
