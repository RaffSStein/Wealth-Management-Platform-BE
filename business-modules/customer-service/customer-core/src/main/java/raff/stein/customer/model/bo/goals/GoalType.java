package raff.stein.customer.model.bo.goals;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoalType {

    private String name;
    private String description;
}
