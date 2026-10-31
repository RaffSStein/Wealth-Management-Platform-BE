package raff.stein.profiler.service.assignment.policy;

/**
 * Where an assignment lands and what it needs before taking effect.
 */
public record AssignmentDecision(
        String bankCode,
        String branchCode,
        boolean requiresApproval,
        boolean requiresRecentLogin) {
}
