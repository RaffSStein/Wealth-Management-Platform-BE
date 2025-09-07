package raff.stein.customer.model.entity.mifid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.audit.entity.BaseEntity;

import java.util.Set;

@Entity
@Table(name = "mifid_question")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MifidQuestionEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false, insertable = false, updatable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private MifidSectionEntity section;

    @Column(name = "section_id", nullable = false)
    @Setter
    private Long sectionId;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<MifidAnswerOptionEntity> answerOptions;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<MifidResponseEntity> responses;

    // Fields
    @Column(nullable = false)
    private String code;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private String questionType;

    @Column(nullable = false)
    private Boolean isRequired;

    @Column
    private Integer orderIndex;


}
