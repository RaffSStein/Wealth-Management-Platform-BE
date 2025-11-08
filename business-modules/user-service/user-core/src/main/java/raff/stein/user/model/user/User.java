package raff.stein.user.model.user;

import lombok.Builder;
import lombok.Data;
import raff.stein.user.model.BranchRole;

import java.util.List;

@Data
@Builder
public class User {

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
    private List<BranchRole> branchRoles;
    private UserSettings userSettings;

    // Additional fields and methods can be added as needed
}
