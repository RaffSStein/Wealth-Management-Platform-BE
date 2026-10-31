package raff.stein.profiler.service.assignment.policy;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;

public final class AssignmentDenied {

    private AssignmentDenied() {
    }

    public static AccessDeniedException because(String reason) {
        return new AccessDeniedException(
                ErrorCode.ROLE_ASSIGNMENT_NOT_ALLOWED,
                String.format(ErrorCode.ROLE_ASSIGNMENT_NOT_ALLOWED.getMessageTemplate(), reason));
    }
}
