package raff.stein.user.model.user;

import lombok.Builder;
import lombok.Data;
import raff.stein.user.model.BranchRole;

import java.util.List;
import java.util.UUID;

/**
 * Enough to find and link a user (lookup, search): no phone, address or birth date.
 */
@Data
@Builder
public class UserSummary {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    // only the role assignments the caller may see
    private List<BranchRole> roleAssignments;
}
