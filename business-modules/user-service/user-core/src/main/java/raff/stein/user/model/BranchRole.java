package raff.stein.user.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchRole {

    private String bankCode;
    private String bankId;
    private String role;

}
