package raff.stein.customer.model.entity.onboarding;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.customer.enumeration.OnboardingStatus;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "customer_onboarding")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOnboardingEntity extends BaseDateEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, insertable = false, updatable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private CustomerEntity customer;

    @Column(name = "customer_id", nullable = false)
    @Setter
    private UUID customerId;

    @OneToMany(mappedBy = "customerOnboarding", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<CustomerOnboardingStepEntity> onboardingSteps;

    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    @Setter
    private OnboardingStatus onboardingStatus;

    @Column(length = 1000)
    @Setter
    private String reason;

    /**
     * Indicates whether this onboarding is actual being performed or not.
     * This is used to differentiate between active and older onboarding processes.
     */
    @Column(nullable = false)
    @Setter
    private boolean isValid;




}
