package raff.stein.customer.model.entity.aml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

import java.time.OffsetDateTime;
import java.util.Set;

@Entity
@Table(name = "aml_verification")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmlVerificationEntity extends BaseDateEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @OneToMany(mappedBy = "amlVerification", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<AmlMatchEntity> matches;

    @OneToMany(mappedBy = "amlVerification", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<AmlDocumentEntity> documents;

    // Fields

    @Column(nullable = false)
    private OffsetDateTime verificationDate;

    @Column(nullable = false)
    private String status;

    @Column
    private String providerName;

    @Column
    private String countryCode;

    @Column
    private String verificationResult;

    @Column
    private Double riskScore;

    @Column
    private String notes;

    @Column
    private OffsetDateTime expiresAt;

}
