package raff.stein.profiler.client.bank;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import raff.stein.profiler.client.bank.config.BankClientConfig;
import raff.stein.profiler.client.bank.model.BankBranchResponse;

@FeignClient(
        name = "bankClient",
        url = "${wmp.clients.bank-service.url}",
        configuration = BankClientConfig.class
)
public interface BankClient {

    @GetMapping(value = "/bank/{bankCode}/branches/{branchCode}", produces = "application/json")
    BankBranchResponse getBranch(@PathVariable("bankCode") String bankCode, @PathVariable("branchCode") String branchCode);
}
