package raff.stein.customer.model.entity.onboarding;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStep;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

@Entity
@Table(name = "customer_onboarding_step")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOnboardingStepEntity extends BaseDateEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_onboarding_id", nullable = false, insertable = false, updatable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private CustomerOnboardingEntity customerOnboarding;

    @Column(name = "customer_onboarding_id", nullable = false)
    @Setter
    private Long customerOnboardingId;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private OnboardingStep step;

    @Column(nullable = false)
    @Setter
    private String status;

    @Column(length = 1000)
    @Setter
    private String reason;

    @Column(nullable = false)
    private int stepOrder;

}
