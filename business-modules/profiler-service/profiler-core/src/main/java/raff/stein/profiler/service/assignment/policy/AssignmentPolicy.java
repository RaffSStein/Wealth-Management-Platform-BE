package raff.stein.profiler.service.assignment.policy;

import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.role.WmpRole;
import raff.stein.profiler.model.assignment.AssignRoleCommand;

/**
 * What a caller holding {@link #callerRole()} may do with role assignments. One implementation per role, picked by
 * {@link AssignmentPolicyDispatcher}. The matrix is documented in docs/domain/process/security/roles-and-permissions.md.
 */
public interface AssignmentPolicy {

    WmpRole callerRole();

    /**
     * Decides bank, branch and approval for an assignment, or throws ROLE_ASSIGNMENT_NOT_ALLOWED.
     */
    AssignmentDecision decideAssignment(WMPContext caller, WmpRole requestedRole, AssignRoleCommand command);

    /**
     * True when the caller may approve or reject a request for this role in this bank.
     */
    boolean mayDecide(WMPContext caller, WmpRole requestedRole, String bankCode);

    boolean mayRevoke(WMPContext caller, WmpRole role, String bankCode, String branchCode);

    /**
     * True when the caller may see assignments in this bank and branch.
     */
    boolean mayView(WMPContext caller, String bankCode, String branchCode);

    boolean requiresRecentLoginToRevoke();
}
