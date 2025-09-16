package raff.stein.bank.model.entity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import raff.stein.bank.model.bo.BankBranch;
import raff.stein.bank.model.entity.BankBranchEntity;


@Mapper(config = BankEntityCommonMapperConfig.class)
public interface BankBranchToBranchEntityMapper {

    BankBranchToBranchEntityMapper MAPPER = Mappers.getMapper(BankBranchToBranchEntityMapper.class);


    BankBranchEntity toEntity(BankBranch bankBranch);

    BankBranch toModel(BankBranchEntity bankBranchEntity);
}
