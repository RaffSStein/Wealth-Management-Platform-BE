package raff.stein.user.model.entity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import raff.stein.user.model.UserSettings;
import raff.stein.user.model.entity.UserSettingsEntity;

@Mapper(config = UserEntityCommonMapperConfig.class)
public interface UserSettingsToUserSettingsEntityMapper {

    UserSettingsToUserSettingsEntityMapper MAPPER = Mappers.getMapper(UserSettingsToUserSettingsEntityMapper.class);

    UserSettingsEntity toUserSettingsEntity(UserSettings userSettings);

    UserSettings toUserSettings(UserSettingsEntity userSettingsEntity);

}
