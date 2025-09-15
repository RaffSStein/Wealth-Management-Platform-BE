package raff.stein.bank.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankBranch {

    private String bankCode;
    private String bankName;
    private String branchCode;
    private String branchName;
    private String swiftCode;
    private String countryCode;
    private String bankType;
    private String status;
    private String bankDescription;
    private String branchDescription;
    private String branchCity;
    private String address;
    private String zipCode;
    private String phoneNumber;
    private String email;

}
