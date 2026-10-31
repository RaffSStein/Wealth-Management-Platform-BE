package raff.stein.profiler.service.assignment.policy;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.role.WmpRole;
import raff.stein.profiler.model.assignment.AssignRoleCommand;

import java.util.Objects;

/**
 * An advisor links customers to its own branch, directly: the daily onboarding flow.
 */
@Component
public class AdvisorAssignmentPolicy implements AssignmentPolicy {

    @Override
    public WmpRole callerRole() {
        return WmpRole.ADVISOR;
    }

    @Override
    public AssignmentDecision decideAssignment(WMPContext caller, WmpRole requestedRole, AssignRoleCommand command) {
        if (requestedRole != WmpRole.CUSTOMER) {
            throw AssignmentDenied.because("an ADVISOR can only assign CUSTOMER");
        }
        if (StringUtils.isNotBlank(command.getBankCode())) {
            throw AssignmentDenied.because("only a PLATFORM_ADMIN can choose the bank");
        }
        if (StringUtils.isBlank(caller.getTenantId()) || !Objects.equals(caller.getBranchCode(), command.getBranchCode())) {
            throw AssignmentDenied.because("an ADVISOR can only assign in its own branch");
        }
        return new AssignmentDecision(caller.getTenantId(), caller.getBranchCode(), false, false);
    }

    @Override
    public boolean mayDecide(WMPContext caller, WmpRole requestedRole, String bankCode) {
        return false;
    }

    @Override
    public boolean mayRevoke(WMPContext caller, WmpRole role, String bankCode, String branchCode) {
        return role == WmpRole.CUSTOMER && mayView(caller, bankCode, branchCode);
    }

    @Override
    public boolean mayView(WMPContext caller, String bankCode, String branchCode) {
        return Objects.equals(caller.getTenantId(), bankCode) && Objects.equals(caller.getBranchCode(), branchCode);
    }

    @Override
    public boolean requiresRecentLoginToRevoke() {
        return false;
    }
}
