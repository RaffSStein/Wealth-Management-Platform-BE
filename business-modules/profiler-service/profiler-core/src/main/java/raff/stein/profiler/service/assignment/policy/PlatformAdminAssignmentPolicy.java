package raff.stein.profiler.service.assignment.policy;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.platformcore.security.role.WmpRole;
import raff.stein.profiler.model.assignment.AssignRoleCommand;
import raff.stein.profiler.model.assignment.AssignmentConstants;

/**
 * A platform admin creates bank admins, in any bank, with four-eyes approval by another platform admin.
 */
@Component
public class PlatformAdminAssignmentPolicy implements AssignmentPolicy {

    @Override
    public WmpRole callerRole() {
        return WmpRole.PLATFORM_ADMIN;
    }

    @Override
    public AssignmentDecision decideAssignment(WMPContext caller, WmpRole requestedRole, AssignRoleCommand command) {
        if (requestedRole != WmpRole.BANK_ADMIN) {
            throw AssignmentDenied.because("a PLATFORM_ADMIN assigns BANK_ADMIN only; bank roles are assigned by bank admins");
        }
        if (StringUtils.isBlank(command.getBankCode()) || AssignmentConstants.PLATFORM_TENANT.equals(command.getBankCode())) {
            throw new RequestValidationException(ErrorCode.REQUEST_VALIDATION_ERROR,
                    String.format(ErrorCode.REQUEST_VALIDATION_ERROR.getMessageTemplate(), "bankCode", command.getBankCode()));
        }
        return new AssignmentDecision(command.getBankCode(), command.getBranchCode(), true, true);
    }

    @Override
    public boolean mayDecide(WMPContext caller, WmpRole requestedRole, String bankCode) {
        return requestedRole == WmpRole.BANK_ADMIN;
    }

    @Override
    public boolean mayRevoke(WMPContext caller, WmpRole role, String bankCode, String branchCode) {
        return role == WmpRole.BANK_ADMIN;
    }

    @Override
    public boolean mayView(WMPContext caller, String bankCode, String branchCode) {
        return true;
    }

    @Override
    public boolean requiresRecentLoginToRevoke() {
        return true;
    }
}
