package raff.stein.user.model;

import lombok.Builder;
import lombok.Data;
import raff.stein.platformcore.model.audit.BaseDate;

import java.util.List;

@Data
@Builder
public class User extends BaseDate {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String country;
    private String city;
    private String address;
    private String gender;
    private String birthDate;
    private List<BankBranch> userBankBranches;
    private UserSettings userSettings;

    // Additional fields and methods can be added as needed
}
