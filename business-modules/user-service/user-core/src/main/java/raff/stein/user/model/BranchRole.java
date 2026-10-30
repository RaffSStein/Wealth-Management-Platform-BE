package raff.stein.user.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchRole {

    private String bankCode;
    private String branchCode;
    private String role;
    // the membership the current token was issued for; computed on read, never stored
    private boolean active;
}
