package raff.stein.customer.model.bo.customer;

import lombok.Builder;
import lombok.Data;
import raff.stein.customer.model.bo.goals.GoalType;
import raff.stein.customer.model.entity.goals.enumeration.GoalTimeline;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CustomerGoals {

    private Long id;
    private GoalType goalType;
    private BigDecimal targetAmount;
    private LocalDate targetDate;
    private String notes;
    private GoalTimeline goalTimeline;
}
