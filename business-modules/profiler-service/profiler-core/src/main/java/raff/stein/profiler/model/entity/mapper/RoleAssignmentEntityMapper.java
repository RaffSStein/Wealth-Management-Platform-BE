package raff.stein.profiler.model.entity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import raff.stein.profiler.model.assignment.RoleAssignment;
import raff.stein.profiler.model.assignment.RoleAssignmentRequest;
import raff.stein.profiler.model.mapper.common.ProfilerCommonMapperConfiguration;
import raff.stein.profiler.model.entity.RoleAssignmentEntity;
import raff.stein.profiler.model.entity.RoleAssignmentRequestEntity;

@Mapper(config = ProfilerCommonMapperConfiguration.class)
public interface RoleAssignmentEntityMapper {

    RoleAssignmentEntityMapper MAPPER = Mappers.getMapper(RoleAssignmentEntityMapper.class);

    @Mapping(target = "grantedAt", source = "createdDate")
    RoleAssignment toRoleAssignment(RoleAssignmentEntity entity);

    RoleAssignmentRequest toRoleAssignmentRequest(RoleAssignmentRequestEntity entity);
}
