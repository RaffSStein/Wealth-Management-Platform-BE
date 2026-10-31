package raff.stein.user.controller.mapper;

import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.UserBranchRoleDTO;
import org.openapitools.model.UserSummaryDTO;
import org.openapitools.model.UserSummaryPageDTO;
import org.openapitools.model.UserDTO;
import org.openapitools.model.UserSettingsDTO;
import raff.stein.user.model.BranchRole;
import raff.stein.user.model.user.User;
import raff.stein.user.model.user.UserSummary;
import raff.stein.user.model.user.UserSummaryPage;
import raff.stein.user.model.user.UserSettings;

import java.util.List;

@Mapper(config = UserControllerCommonMapperConfig.class)
public interface UserToUserDTOMapper {

    UserToUserDTOMapper MAPPER = Mappers.getMapper(UserToUserDTOMapper.class);

    // Top-level mappings
    @Mapping(target = "branchRoles", source = "userBranchRoles")
    User toUser(UserDTO userDTO);

    @Mapping(target = "userBranchRoles", source = "branchRoles")
    UserDTO toUserDto(User user);

    // Element mappings for branch roles
    BranchRole toBranchRole(UserBranchRoleDTO userBranchRoleDTO);

    UserBranchRoleDTO toUserBranchRoleDto(BranchRole branchRole);

    // List mappings to ensure null lists become empty (safer for downstream usage)
    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<BranchRole> toBranchRoles(List<UserBranchRoleDTO> dtos);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<UserBranchRoleDTO> toUserBranchRoleDtos(List<BranchRole> domains);

    UserSettings toUserSettings(UserSettingsDTO dto);

    UserSettingsDTO toUserSettingsDto(UserSettings domain);

    // Lookup and search results
    UserSummaryDTO toUserSummaryDto(UserSummary userSummary);

    UserSummaryPageDTO toUserSummaryPageDto(UserSummaryPage userSummaryPage);
}
