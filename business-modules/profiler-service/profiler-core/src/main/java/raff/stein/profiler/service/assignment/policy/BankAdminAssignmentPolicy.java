package raff.stein.profiler.service.assignment.policy;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.role.WmpRole;
import raff.stein.profiler.model.assignment.AssignRoleCommand;

import java.util.Objects;

/**
 * A bank admin manages its own bank: customers directly, advisors with four-eyes approval.
 */
@Component
public class BankAdminAssignmentPolicy implements AssignmentPolicy {

    @Override
    public WmpRole callerRole() {
        return WmpRole.BANK_ADMIN;
    }

    @Override
    public AssignmentDecision decideAssignment(WMPContext caller, WmpRole requestedRole, AssignRoleCommand command) {
        if (requestedRole != WmpRole.CUSTOMER && requestedRole != WmpRole.ADVISOR) {
            throw AssignmentDenied.because("a BANK_ADMIN can assign CUSTOMER or ADVISOR only");
        }
        if (StringUtils.isNotBlank(command.getBankCode())) {
            throw AssignmentDenied.because("only a PLATFORM_ADMIN can choose the bank");
        }
        if (StringUtils.isBlank(caller.getTenantId())) {
            throw AssignmentDenied.because("the token has no bank");
        }
        final boolean staff = requestedRole.isStaff();
        return new AssignmentDecision(caller.getTenantId(), command.getBranchCode(), staff, staff);
    }

    @Override
    public boolean mayDecide(WMPContext caller, WmpRole requestedRole, String bankCode) {
        return requestedRole == WmpRole.ADVISOR && Objects.equals(caller.getTenantId(), bankCode);
    }

    @Override
    public boolean mayRevoke(WMPContext caller, WmpRole role, String bankCode, String branchCode) {
        return (role == WmpRole.CUSTOMER || role == WmpRole.ADVISOR) && Objects.equals(caller.getTenantId(), bankCode);
    }

    @Override
    public boolean mayView(WMPContext caller, String bankCode, String branchCode) {
        return Objects.equals(caller.getTenantId(), bankCode);
    }

    @Override
    public boolean requiresRecentLoginToRevoke() {
        return true;
    }
}
