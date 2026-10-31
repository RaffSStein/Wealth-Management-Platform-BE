package raff.stein.profiler.client.bank.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The fields of bank-service's BankBranchDto that profiler needs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BankBranchResponse(String bankCode, String branchCode, String status) {
}
