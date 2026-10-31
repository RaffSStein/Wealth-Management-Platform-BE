package raff.stein.profiler.model.assignment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignmentResult {

    private Outcome outcome;
    private RoleAssignment assignment;
    private RoleAssignmentRequest request;

    public enum Outcome {
        ASSIGNED,
        PENDING_APPROVAL
    }

    public static AssignmentResult assigned(RoleAssignment assignment) {
        return AssignmentResult.builder().outcome(Outcome.ASSIGNED).assignment(assignment).build();
    }

    public static AssignmentResult pending(RoleAssignmentRequest request) {
        return AssignmentResult.builder().outcome(Outcome.PENDING_APPROVAL).request(request).build();
    }
}
