package raff.stein.user.model.entity.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import raff.stein.user.model.entity.BranchUserEntity;
import raff.stein.user.model.entity.UserEntity;
import raff.stein.user.model.user.User;

import java.time.LocalDate;

@Mapper(
        config = UserEntityCommonMapperConfig.class,
        uses = {
                BranchUserToBranchUserEntity.class,
                UserSettingsToUserSettingsEntityMapper.class
        }
)
public interface UserToUserEntityMapper {

    UserToUserEntityMapper MAPPER = Mappers.getMapper(UserToUserEntityMapper.class);

    @Mapping(target = "bankBranchUsers", source = "branchRoles")
    @Mapping(target = "birthDate", source = "birthDate", qualifiedByName = "toLocalDate")
    UserEntity toUserEntity(User user);

    @Mapping(target = "branchRoles", source = "bankBranchUsers")
    @Mapping(target = "birthDate", source = "birthDate", qualifiedByName = "toStringDate")
    User toUser(UserEntity userEntity);

    @Named("toLocalDate")
    default LocalDate toLocalDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        return LocalDate.parse(date);
    }

    @Named("toStringDate")
    default String toStringDate(LocalDate date) {
        return date == null ? null : date.toString();
    }

    @AfterMapping
    default void linkUserInBranchUsers(User source, @MappingTarget UserEntity target) {
        if (target.getBankBranchUsers() != null) {
            for (BranchUserEntity bue : target.getBankBranchUsers()) {
                bue.setUser(target);
            }
        }
    }
}
