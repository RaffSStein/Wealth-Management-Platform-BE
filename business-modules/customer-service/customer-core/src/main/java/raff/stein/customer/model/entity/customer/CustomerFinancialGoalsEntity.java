package raff.stein.customer.model.entity.customer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.customer.model.entity.goals.GoalTypeEntity;
import raff.stein.customer.model.entity.goals.enumeration.GoalTimeline;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customer_financial_goals")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFinancialGoalsEntity extends BaseDateEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_type_id", nullable = false,  insertable = false, updatable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    @Setter
    private GoalTypeEntity goalType;

    @Column(name = "goal_type_id", nullable = false)
    @Setter
    private Integer goalTypeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false,  insertable = false, updatable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private CustomerEntity customer;

    @Column(name = "customer_id", nullable = false)
    @Setter
    private UUID customerId;

    // Fields

    @Column(nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private GoalTimeline goalTimeline;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal targetAmount;

    @Column(nullable = false)
    private LocalDate targetDate;

    public void updateFrom(CustomerFinancialGoalsEntity source) {
        this.goalTimeline = source.getGoalTimeline();
        this.targetAmount = source.getTargetAmount();
        this.targetDate = source.getTargetDate();
    }



}
