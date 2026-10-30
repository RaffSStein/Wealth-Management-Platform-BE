package raff.stein.customer.model.entity.aml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.tenant.TenantScopedEntity;

@Entity
@Table(name = "aml_match")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmlMatchEntity extends TenantScopedEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aml_verification_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private AmlVerificationEntity amlVerification;

    // Fields

    @Column(nullable = false)
    private String listType;

    @Column(nullable = false)
    private String listSource;

    @Column(nullable = false)
    private String matchName;

    @Column
    private Double matchScore;

    @Column(length = 1000)
    private String matchDetails;


}
