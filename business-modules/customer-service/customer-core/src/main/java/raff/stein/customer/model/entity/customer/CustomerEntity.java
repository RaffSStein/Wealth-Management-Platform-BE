package raff.stein.customer.model.entity.customer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.customer.model.entity.aml.AmlVerificationEntity;
import raff.stein.customer.model.entity.customer.enumeration.CustomerStatus;
import raff.stein.customer.model.entity.customer.enumeration.CustomerType;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEntity extends BaseDateEntity<UUID> {

    //TODO: split customer types specific fields into separate entities if needed

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    //TODO: consider using a separate entity for user reference if needed, a user may access as different customers
    @Column(nullable = false)
    private UUID userId;        // Reference to the user in the platform

    // Relationships

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<CustomerFinancialEntity> customerFinancials;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<CustomerFinancialGoalsEntity> customerFinancialGoals;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<AmlVerificationEntity> amlVerifications;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<MifidFillingEntity> mifidFillings;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<CustomerOnboardingEntity> customerOnboardingStatuses;

    // Fields

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerType customerType;

    @Column
    private String firstName; // Only for individual

    @Column
    private String lastName; // Only for individual

    @Column
    private LocalDate dateOfBirth;

    @Column
    private String gender;

    @Column(length = 2)
    private String nationality; // ISO 3166-1 alpha-2

    @Column
    private String companyName; // Only for corporate

    @Column(nullable = false)
    private String taxId;

    @Column
    private String phoneNumber;

    @Column
    private String addressLine1;

    @Column
    private String addressLine2; // Optional

    @Column
    private String city;

    @Column
    private String zipCode;

    @Column(length = 2)
    private String country; // ISO 3166-1 alpha-2

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerStatus customerStatus;

    @Column
    private LocalDate onboardingDate;

}
