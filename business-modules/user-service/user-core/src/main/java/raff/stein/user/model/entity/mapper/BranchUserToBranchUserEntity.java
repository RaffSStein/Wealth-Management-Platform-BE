package raff.stein.user.model.entity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import raff.stein.user.model.BranchRole;
import raff.stein.user.model.entity.BranchUserEntity;

@Mapper(config = UserEntityCommonMapperConfig.class)
public interface BranchUserToBranchUserEntity {

    BranchUserToBranchUserEntity MAPPER = Mappers.getMapper(BranchUserToBranchUserEntity.class);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "notes", ignore = true)
    BranchUserEntity toBranchUserEntity(BranchRole branchRole);

    BranchRole toBranchRole(BranchUserEntity branchUserEntity);

}
