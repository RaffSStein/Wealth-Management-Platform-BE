package raff.stein.customer.model.bo.mifid.config;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MifidQuestion {

    private Long id;
    private String text;
    private String questionType;
    private Boolean isRequired;
    private Integer orderIndex;
    private List<MifidAnswerOption> answerOptions;
}

