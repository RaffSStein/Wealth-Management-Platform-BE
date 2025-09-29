package raff.stein.user.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.UserBranchRoleDTO;
import org.openapitools.model.UserDTO;
import org.openapitools.model.UserSettingsDTO;
import raff.stein.user.model.BranchRole;
import raff.stein.user.model.User;
import raff.stein.user.model.UserSettings;

@Mapper(config = UserControllerCommonMapperConfig.class)
public interface UserToUserDTOMapper {

    UserToUserDTOMapper MAPPER = Mappers.getMapper(UserToUserDTOMapper.class);

    // Top-level mappings
    User toUser(UserDTO userDTO);

    UserDTO toUserDto(User user);

    // Nested mappings for branch roles
    BranchRole toBranchRole(UserBranchRoleDTO userBranchRoleDTO);

    UserBranchRoleDTO toUserBranchRoleDto(BranchRole branchRole);

    UserSettings toUserSettings(UserSettingsDTO dto);

    UserSettingsDTO toUserSettingsDto(UserSettings domain);
}
