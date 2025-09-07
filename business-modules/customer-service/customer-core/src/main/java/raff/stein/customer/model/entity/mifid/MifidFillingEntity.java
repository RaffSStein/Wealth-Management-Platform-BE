package raff.stein.customer.model.entity.mifid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.customer.model.entity.customer.CustomerEntity;
import raff.stein.customer.model.entity.mifid.enumeration.MifidFillingStatus;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "mifid_filling")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MifidFillingEntity extends BaseDateEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, insertable = false, updatable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private CustomerEntity customer;

    @Column(name = "customer_id", nullable = false)
    @Setter
    private UUID customerId;

    @OneToMany(mappedBy = "filling", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    @Setter
    private Set<MifidResponseEntity> responses;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questionnaire_id", nullable = false, insertable = false, updatable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private MifidQuestionnaireEntity questionnaire;

    @Column(name = "questionnaire_id", nullable = false)
    @Setter
    private Long questionnaireId;

    // Fields

    @Column(nullable = false)
    private LocalDateTime fillingDate;

    @Column(length = 100)
    private String calculatedRiskProfile;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Setter
    private MifidFillingStatus status;


    public void initializeMifidFirstSave() {
        this.fillingDate = LocalDateTime.now();
        this.status = MifidFillingStatus.DRAFT;
    }

    public void updateMifidFields(MifidFillingStatus newStatus) {
        this.fillingDate = LocalDateTime.now();
        this.status = newStatus;
    }


}
