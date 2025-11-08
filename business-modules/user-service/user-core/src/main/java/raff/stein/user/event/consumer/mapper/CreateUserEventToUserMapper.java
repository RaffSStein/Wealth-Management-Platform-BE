package raff.stein.user.event.consumer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.CreateUserEvent;
import raff.stein.platformcore.model.mapper.configuration.CommonMapperConfiguration;
import raff.stein.user.model.user.User;

@Mapper(config = CommonMapperConfiguration.class)
public interface CreateUserEventToUserMapper {

    CreateUserEventToUserMapper MAPPER =  Mappers.getMapper(CreateUserEventToUserMapper.class);

    @Mapping(target = ".", source = "user")
    User toUser(CreateUserEvent createUserEvent);

}
