package raff.stein.customer.model.entity.mifid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.audit.entity.BaseEntity;

import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "mifid_questionnaire")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MifidQuestionnaireEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships

    @OneToMany(mappedBy = "questionnaire", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<MifidSectionEntity> sections;

    @OneToMany(mappedBy = "questionnaire", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<MifidFillingEntity> fillings;

    @OneToMany(mappedBy = "questionnaire", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<RiskProfileConfigurationEntity> riskProfiles;

    // Fields

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate validTo;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 20)
    private String questionnaireVersion;

    @Column(nullable = false, length = 30)
    private String status;


}
