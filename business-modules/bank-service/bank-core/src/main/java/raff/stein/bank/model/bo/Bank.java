package raff.stein.bank.model.bo;

/**
 * A bank (tenant). There is no bank table: a bank is derived from its branches in bank_branches.
 */
public record Bank(String bankCode, String bankName, String countryCode, String bankType) {
}
