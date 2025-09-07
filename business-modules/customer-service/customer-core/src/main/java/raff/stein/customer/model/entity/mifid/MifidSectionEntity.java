package raff.stein.customer.model.entity.mifid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.audit.entity.BaseEntity;

import java.util.Set;

@Entity
@Table(name = "mifid_section")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MifidSectionEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<MifidQuestionEntity> questions;

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

    @Column(nullable = false, length = 200)
    private String title;

    @Column
    private Integer orderIndex;


}
