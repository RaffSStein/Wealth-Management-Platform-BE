package raff.stein.customer.model.entity.mifid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.platformcore.model.audit.entity.BaseEntity;

import java.time.LocalDate;

@Entity
@Table(name = "mifid_response")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MifidResponseEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answer_option_id",  nullable = false, insertable = false, updatable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    @Setter
    private MifidAnswerOptionEntity answerOption;

    @Column(name = "answer_option_id", nullable = false)
    @Setter
    private Long answerOptionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, insertable = false, updatable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    @Setter
    private MifidQuestionEntity question;

    @Column(name = "question_id", nullable = false)
    @Setter
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filling_id", nullable = false, insertable = false, updatable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private MifidFillingEntity filling;

    @Column(name = "filling_id", nullable = false)
    @Setter
    private Long fillingId;

    // Fields

    @Column(name = "free_text", length = 1000)
    private String freeText;

    @Column(name = "numeric_value")
    private Double numericValue;

    @Column(name = "date_value")
    private LocalDate dateValue;

}
