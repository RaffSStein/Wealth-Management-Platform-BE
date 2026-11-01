package raff.stein.customer.model.entity.customer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.customer.model.entity.aml.AmlVerificationEntity;
import raff.stein.customer.model.entity.customer.enumeration.CustomerStatus;
import raff.stein.customer.model.entity.customer.enumeration.CustomerType;
import raff.stein.customer.model.entity.mifid.MifidFillingEntity;
import raff.stein.customer.model.entity.onboarding.CustomerOnboardingEntity;
import raff.stein.platformcore.model.tenant.TenantScopedEntity;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "customers",
        // one record per bank for a client; a record not linked yet has no user, and NULLs don't collide
        uniqueConstraints = {
                @UniqueConstraint(name = CustomerEntity.TENANT_USER_CONSTRAINT, columnNames = {"tenant_id", "user_id"}),
                @UniqueConstraint(name = CustomerEntity.TENANT_EMAIL_CONSTRAINT, columnNames = {"tenant_id", "email"})
        })
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEntity extends TenantScopedEntity<UUID> {

    public static final String TENANT_USER_CONSTRAINT = "uk_customers_tenant_user";
    public static final String TENANT_EMAIL_CONSTRAINT = "uk_customers_tenant_email";

    //TODO: split customer types specific fields into separate entities if needed

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The client's login, never the staff member who created the record. Null until the client holds the CUSTOMER role
     * in this branch; the role assignment events keep it in step.
     */
    @Column
    @Setter
    private UUID userId;

    /**
     * The client's email, lowercase. It links the record to a login. Null only on records older than this field.
     */
    @Column
    private String email;

    /**
     * The branch the customer belongs to, from the token that created it. Limits which advisors reach it.
     */
    @Column(nullable = false)
    private String branchCode;

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
    @Setter
    private String firstName; // Only for individual

    @Column
    @Setter
    private String lastName; // Only for individual

    @Column
    @Setter
    private LocalDate dateOfBirth;

    @Column
    @Setter
    private String gender;

    @Column(length = 2)
    @Setter
    private String nationality; // ISO 3166-1 alpha-2

    @Column
    private String companyName; // Only for corporate

    @Column(nullable = false)
    @Setter
    private String taxId;

    @Column
    @Setter
    private String phoneNumber;

    @Column
    @Setter
    private String addressLine1;

    @Column
    @Setter
    private String addressLine2; // Optional

    @Column
    @Setter
    private String city;

    @Column
    @Setter
    private String zipCode;

    @Column(length = 2)
    @Setter
    private String country; // ISO 3166-1 alpha-2

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private CustomerStatus customerStatus;

    @Column
    private LocalDate onboardingDate;

    // latest submitted MiFID result, denormalized for customer reads (dashboards, advisor lists);
    // the full history stays on the MiFID fillings
    @Column(length = 100)
    @Setter
    private String riskProfile;

    @Column
    @Setter
    private Integer riskScore;

}
