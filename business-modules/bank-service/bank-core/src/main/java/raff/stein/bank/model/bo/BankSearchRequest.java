package raff.stein.bank.model.bo;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Pageable;

@Data
@Builder
public class BankSearchRequest {

    private Pageable pageable;
    private String bankCode;
    private String bankName;
    private String branchCode;
    private String swiftCode;
    private String countryCode;
    private String bankType;
    private String branchCity;
    private String zipCode;
}
