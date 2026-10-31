package raff.stein.profiler.client.bank;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;
import raff.stein.platformcore.exception.types.unavailable.DependencyUnavailableException;
import raff.stein.profiler.client.bank.model.BankBranchResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankBranchValidator {

    private static final String ACTIVE = "ACTIVE";

    private final BankClient bankClient;

    /**
     * 404 when the bank has no such branch, 400 when it isn't ACTIVE, 503 when bank-service can't answer.
     */
    public void requireActiveBranch(String bankCode, String branchCode) {
        final BankBranchResponse branch;
        try {
            branch = bankClient.getBranch(bankCode, branchCode);
        } catch (FeignException.NotFound e) {
            throw GenericObjectNotFoundException.with(bankCode + "/" + branchCode).get();
        } catch (FeignException e) {
            log.error("bank-service lookup failed for branch [{}/{}] with status [{}]", bankCode, branchCode, e.status());
            throw new DependencyUnavailableException("bank-service", e);
        }
        if (branch == null || !ACTIVE.equalsIgnoreCase(branch.status())) {
            throw new RequestValidationException(ErrorCode.REQUEST_VALIDATION_ERROR,
                    String.format(ErrorCode.REQUEST_VALIDATION_ERROR.getMessageTemplate(),
                            "branch " + bankCode + "/" + branchCode, branch == null ? null : branch.status()));
        }
    }
}
