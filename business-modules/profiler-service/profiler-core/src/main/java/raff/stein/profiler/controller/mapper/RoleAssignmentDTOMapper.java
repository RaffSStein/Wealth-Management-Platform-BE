package raff.stein.profiler.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.openapitools.model.AssignRoleRequestDTO;
import org.openapitools.model.AssignRoleResultDTO;
import org.openapitools.model.RoleAssignmentDTO;
import org.openapitools.model.RoleAssignmentRequestDTO;
import org.openapitools.model.RoleAssignmentRequestStatusDTO;
import raff.stein.profiler.model.assignment.AssignRoleCommand;
import raff.stein.profiler.model.assignment.AssignmentResult;
import raff.stein.profiler.model.assignment.RoleAssignment;
import raff.stein.profiler.model.assignment.RoleAssignmentRequest;
import raff.stein.profiler.model.entity.enumeration.RoleAssignmentRequestStatus;
import raff.stein.profiler.model.mapper.common.ProfilerCommonMapperConfiguration;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(config = ProfilerCommonMapperConfiguration.class)
public interface RoleAssignmentDTOMapper {

    RoleAssignmentDTOMapper MAPPER = Mappers.getMapper(RoleAssignmentDTOMapper.class);

    AssignRoleCommand toAssignRoleCommand(AssignRoleRequestDTO dto);

    AssignRoleResultDTO toAssignRoleResultDTO(AssignmentResult result);

    RoleAssignmentDTO toRoleAssignmentDTO(RoleAssignment assignment);

    RoleAssignmentRequestDTO toRoleAssignmentRequestDTO(RoleAssignmentRequest request);

    RoleAssignmentRequestStatus toRequestStatus(RoleAssignmentRequestStatusDTO status);

    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
