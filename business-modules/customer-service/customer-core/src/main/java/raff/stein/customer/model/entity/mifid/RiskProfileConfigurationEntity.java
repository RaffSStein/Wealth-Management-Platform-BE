package raff.stein.customer.model.entity.mifid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.audit.entity.BaseEntity;

@Entity
@Table(name = "risk_profile_configuration")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskProfileConfigurationEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships

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

    @Column(nullable = false, length = 50)
    private String profileCode;

    @Column(nullable = false)
    private Integer minScore;

    @Column(nullable = false)
    private Integer maxScore;

    @Column(length = 100)
    private String profileLabel;

}
