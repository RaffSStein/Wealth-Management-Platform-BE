package raff.stein.user.event.producer.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.UserCreatedEvent;
import raff.stein.platformcore.model.mapper.configuration.CommonMapperConfiguration;
import raff.stein.user.model.BranchRole;
import raff.stein.user.model.user.User;

import java.time.LocalDate;
import java.util.List;

@Mapper(config = CommonMapperConfiguration.class)
public interface UserToUserCreatedEventMapper {

    UserToUserCreatedEventMapper MAPPER = Mappers.getMapper(UserToUserCreatedEventMapper.class);

    @Mapping(target = "user", source = ".")
    @Mapping(target = "userId", source = "id")
    UserCreatedEvent toUserCreatedEvent(User user);

    // Map domain User -> event User
    @Mapping(target = "userBankBranches", source = "branchRoles")
    @Mapping(target = "birthDate", source = "birthDate", qualifiedByName = "mapBirthDate")
    org.openapitools.model.User toEventUser(User user);

    // Map a single BranchRole -> UserBankBranch
    @Mapping(target = "bankCode", source = "bankCode")
    @Mapping(target = "branchCode", source = "bankId")
    @Mapping(target = "role", source = "role")
    org.openapitools.model.UserBankBranch toUserBankBranch(BranchRole role);

    // Map list of BranchRole -> list of UserBankBranch
    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<org.openapitools.model.UserBankBranch> toUserBankBranches(List<BranchRole> roles);

    @Named("mapBirthDate")
    default LocalDate mapBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(birthDate);
    }
}
