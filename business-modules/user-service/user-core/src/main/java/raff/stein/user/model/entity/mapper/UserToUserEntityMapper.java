package raff.stein.user.model.entity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import raff.stein.user.model.User;
import raff.stein.user.model.entity.UserEntity;

@Mapper(
        config = UserEntityCommonMapperConfig.class,
        uses = {
                BranchUserToBranchUserEntity.class,
                UserSettingsToUserSettingsEntityMapper.class
        }
)
public interface UserToUserEntityMapper {

    UserToUserEntityMapper MAPPER = Mappers.getMapper(UserToUserEntityMapper.class);

    UserEntity toUserEntity(User user);

    User toUser(UserEntity userEntity);
}
