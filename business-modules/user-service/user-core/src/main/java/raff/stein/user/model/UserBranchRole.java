package raff.stein.user.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserBranchRole {

    private String bankCode;
    private String role;

}
