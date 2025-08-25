package raff.stein.customer.model.entity.mifid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.audit.entity.BaseEntity;

import java.util.Set;

@Entity
@Table(name = "mifid_answer_option")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MifidAnswerOptionEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, insertable = false, updatable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private MifidQuestionEntity question;

    @Column(name = "question_id", nullable = false)
    @Setter
    private Long questionId;

    @OneToMany(mappedBy = "answerOption", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<MifidResponseEntity> responses;

    // Fields
    @Column(nullable = false)
    private String code;

    @Column(nullable = false, length = 500)
    private String optionText;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private Integer orderIndex;

}
