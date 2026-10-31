package raff.stein.profiler.controller;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.RoleAssignmentApi;
import org.openapitools.model.AssignRoleRequestDTO;
import org.openapitools.model.AssignRoleResultDTO;
import org.openapitools.model.RejectRoleAssignmentRequestDTO;
import org.openapitools.model.RoleAssignmentDTO;
import org.openapitools.model.RoleAssignmentRequestDTO;
import org.openapitools.model.RoleAssignmentRequestStatusDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import raff.stein.profiler.controller.mapper.RoleAssignmentDTOMapper;
import raff.stein.profiler.model.assignment.AssignmentResult;
import raff.stein.profiler.service.assignment.RoleAssignmentService;

import java.util.List;
import java.util.UUID;

/**
 * Coarse role gate only: the assignment matrix itself is enforced by {@link RoleAssignmentService}.
 */
@RestController
@RequiredArgsConstructor
public class RoleAssignmentController implements RoleAssignmentApi {

    private final RoleAssignmentService roleAssignmentService;
    private static final RoleAssignmentDTOMapper mapper = RoleAssignmentDTOMapper.MAPPER;

    @Override
    @PreAuthorize("hasAnyRole('ADVISOR','BANK_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<AssignRoleResultDTO> assignRole(AssignRoleRequestDTO assignRoleRequestDTO) {
        final AssignmentResult result = roleAssignmentService.assign(mapper.toAssignRoleCommand(assignRoleRequestDTO));
        final HttpStatus status = result.getOutcome() == AssignmentResult.Outcome.ASSIGNED
                ? HttpStatus.CREATED
                : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(mapper.toAssignRoleResultDTO(result));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADVISOR','BANK_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<List<RoleAssignmentDTO>> listRoleAssignments(UUID userId) {
        return ResponseEntity.ok(roleAssignmentService.listAssignments(userId).stream()
                .map(mapper::toRoleAssignmentDTO)
                .toList());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADVISOR','BANK_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<Void> revokeRoleAssignment(Long assignmentId) {
        roleAssignmentService.revoke(assignmentId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAnyRole('BANK_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<List<RoleAssignmentRequestDTO>> listRoleAssignmentRequests(RoleAssignmentRequestStatusDTO status) {
        return ResponseEntity.ok(roleAssignmentService.listRequests(mapper.toRequestStatus(status)).stream()
                .map(mapper::toRoleAssignmentRequestDTO)
                .toList());
    }

    @Override
    @PreAuthorize("hasAnyRole('BANK_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<RoleAssignmentDTO> approveRoleAssignmentRequest(Long requestId) {
        return ResponseEntity.ok(mapper.toRoleAssignmentDTO(roleAssignmentService.approve(requestId)));
    }

    @Override
    @PreAuthorize("hasAnyRole('BANK_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<RoleAssignmentRequestDTO> rejectRoleAssignmentRequest(
            Long requestId, RejectRoleAssignmentRequestDTO rejectRoleAssignmentRequestDTO) {
        return ResponseEntity.ok(mapper.toRoleAssignmentRequestDTO(
                roleAssignmentService.reject(requestId, rejectRoleAssignmentRequestDTO.getReason())));
    }
}
