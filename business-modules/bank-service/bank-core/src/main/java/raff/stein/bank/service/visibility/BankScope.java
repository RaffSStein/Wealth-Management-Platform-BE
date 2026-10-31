package raff.stein.bank.service.visibility;

/**
 * Which banks a caller may read: all of them, exactly one, or none.
 */
public record BankScope(boolean all, String bankCode) {

    public static BankScope allBanks() {
        return new BankScope(true, null);
    }

    public static BankScope only(String bankCode) {
        return new BankScope(false, bankCode);
    }

    public static BankScope none() {
        return new BankScope(false, null);
    }

    public boolean isNone() {
        return !all && bankCode == null;
    }

    public boolean includes(String candidate) {
        return all || (bankCode != null && bankCode.equals(candidate));
    }
}
